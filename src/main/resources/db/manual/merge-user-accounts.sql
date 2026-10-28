/*
 * One-time SQL Server migration for the UserAccount -> entity.User merge.
 * Back up the database before running. The old account and topic-link tables are
 * retained as archives; all application relationships are moved to dbo.users.
 */
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.user_accounts', N'U') IS NULL
BEGIN
    IF OBJECT_ID(N'dbo.user_roles', N'U') IS NOT NULL
    BEGIN
        IF OBJECT_ID(N'dbo.user_consultant_titles', N'U') IS NULL
            CREATE TABLE dbo.user_consultant_titles (
                user_id bigint NOT NULL,
                title varchar(20) NOT NULL,
                CONSTRAINT PK_user_consultant_titles PRIMARY KEY (user_id, title),
                CONSTRAINT FK_user_consultant_titles_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
            );
        EXEC sys.sp_executesql N'
            INSERT INTO dbo.user_consultant_titles(user_id, title)
            SELECT DISTINCT legacy.user_id,
                   CASE legacy.role WHEN ''MANAGER'' THEN ''TEAM_LEADER'' ELSE legacy.role END
            FROM dbo.user_roles AS legacy
            WHERE legacy.role IN (''STUDENT'', ''ADVISOR'', ''LECTURER'', ''STAFF'', ''TEAM_LEADER'', ''MANAGER'')
              AND NOT EXISTS (
                  SELECT 1 FROM dbo.user_consultant_titles AS existing
                  WHERE existing.user_id = legacy.user_id
                    AND existing.title = CASE legacy.role WHEN ''MANAGER'' THEN ''TEAM_LEADER'' ELSE legacy.role END
              );
            UPDATE target SET student = 1
            FROM dbo.users AS target
            WHERE EXISTS (SELECT 1 FROM dbo.user_roles AS legacy WHERE legacy.user_id = target.id AND legacy.role = ''STUDENT'');
            UPDATE users SET role = CASE WHEN role IN (''ADVISOR'', ''LECTURER'', ''STAFF'', ''TEAM_LEADER'') THEN ''CONSULTANT''
                                         WHEN role = ''STUDENT'' THEN ''USER'' ELSE role END;
            UPDATE target SET role = ''CONSULTANT''
            FROM dbo.users AS target
            WHERE target.role = ''USER''
              AND EXISTS (SELECT 1 FROM dbo.user_consultant_titles AS title
                          WHERE title.user_id = target.id AND title.title <> ''STUDENT'');
        ';
    END;
    PRINT 'dbo.user_accounts is absent; the account merge has already been applied.';
    RETURN;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    IF EXISTS (
        SELECT LOWER(LTRIM(RTRIM(email)))
        FROM dbo.user_accounts
        GROUP BY LOWER(LTRIM(RTRIM(email)))
        HAVING COUNT(*) > 1
    )
        THROW 51000, 'Duplicate normalized email in dbo.user_accounts; resolve duplicates before migration.', 1;

    IF EXISTS (SELECT 1 FROM dbo.user_accounts WHERE NULLIF(LTRIM(RTRIM(email)), '') IS NULL)
        THROW 51001, 'A row in dbo.user_accounts has no email; resolve it before migration.', 1;
    IF EXISTS (SELECT 1 FROM dbo.user_accounts WHERE LEN(email) > 150)
        THROW 51004, 'An email in dbo.user_accounts exceeds 150 characters; resolve it before migration.', 1;

    IF EXISTS (
        SELECT source.id
        FROM dbo.user_accounts AS source
        INNER JOIN dbo.users AS target
            ON LOWER(LTRIM(RTRIM(target.email))) = LOWER(LTRIM(RTRIM(source.email)))
            OR LOWER(LTRIM(RTRIM(target.deleted_email))) = LOWER(LTRIM(RTRIM(source.email)))
        GROUP BY source.id
        HAVING COUNT(*) > 1
    )
        THROW 51002, 'A legacy account matches multiple rows in dbo.users; resolve duplicates before migration.', 1;

    IF EXISTS (
        SELECT 1
        FROM sys.foreign_keys AS fk
        INNER JOIN sys.tables AS parent_table ON parent_table.object_id = fk.parent_object_id
        WHERE fk.referenced_object_id = OBJECT_ID(N'dbo.user_accounts')
          AND parent_table.name NOT IN (N'questions', N'answers', N'question_transfers', N'user_faculty_topics')
    )
        THROW 51003, 'An unhandled table references dbo.user_accounts; extend the migration before running it.', 1;

    IF COL_LENGTH(N'dbo.users', N'title') IS NULL
        ALTER TABLE dbo.users ADD title nvarchar(160) NULL;
    IF COL_LENGTH(N'dbo.users', N'student_code') IS NULL
        ALTER TABLE dbo.users ADD student_code nvarchar(30) NULL;
    IF COL_LENGTH(N'dbo.users', N'faculty_id') IS NULL
        ALTER TABLE dbo.users ADD faculty_id bigint NULL;

    IF OBJECT_ID(N'dbo.user_consultant_titles', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.user_consultant_titles (
            user_id bigint NOT NULL,
            title varchar(20) NOT NULL,
            CONSTRAINT PK_user_consultant_titles PRIMARY KEY (user_id, title),
            CONSTRAINT FK_user_consultant_titles_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
        );
    END;

    IF OBJECT_ID(N'dbo.user_assigned_topics', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.user_assigned_topics (
            user_id bigint NOT NULL,
            topic_id bigint NOT NULL,
            CONSTRAINT PK_user_assigned_topics PRIMARY KEY (user_id, topic_id),
            CONSTRAINT FK_user_assigned_topics_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
            CONSTRAINT FK_user_assigned_topics_topics FOREIGN KEY (topic_id) REFERENCES dbo.faculty_topics(id)
        );
    END;

    DECLARE @UserIdMap TABLE (
        legacy_user_id bigint NOT NULL PRIMARY KEY,
        user_id bigint NOT NULL
    );

    MERGE dbo.users AS target
    USING dbo.user_accounts AS source
       ON LOWER(LTRIM(RTRIM(target.email))) = LOWER(LTRIM(RTRIM(source.email)))
       OR LOWER(LTRIM(RTRIM(target.deleted_email))) = LOWER(LTRIM(RTRIM(source.email)))
    WHEN MATCHED THEN UPDATE SET
        target.full_name = CASE WHEN NULLIF(LTRIM(RTRIM(target.full_name)), '') IS NULL
                                THEN LEFT(COALESCE(NULLIF(LTRIM(RTRIM(source.display_name)), ''), source.email), 100)
                                ELSE target.full_name END,
        target.password_hash = COALESCE(NULLIF(target.password_hash, ''), NULLIF(source.password_hash, '')),
        target.student = CASE WHEN CHARINDEX(',STUDENT,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0
                              THEN 1 ELSE target.student END,
        target.currently_studying = CASE
            WHEN CHARINDEX(',STUDENT,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0
             AND NULLIF(LTRIM(RTRIM(source.student_code)), '') IS NOT NULL THEN 1
            ELSE target.currently_studying END,
        target.student_id = COALESCE(NULLIF(target.student_id, ''), LEFT(NULLIF(source.student_code, ''), 30)),
        target.student_code = COALESCE(NULLIF(target.student_code, ''), LEFT(NULLIF(source.student_code, ''), 30)),
        target.title = COALESCE(NULLIF(target.title, ''), LEFT(NULLIF(source.title, ''), 160)),
        target.faculty_id = COALESCE(target.faculty_id, source.faculty_id),
        target.enabled = CASE WHEN target.enabled = 0 OR COALESCE(source.enabled, 1) = 0 THEN 0 ELSE 1 END,
        target.deleted = CASE WHEN COALESCE(target.deleted, 0) = 1 OR COALESCE(source.deleted, 0) = 1 THEN 1 ELSE 0 END,
        target.deleted_email = COALESCE(target.deleted_email, source.deleted_email,
            CASE WHEN COALESCE(source.deleted, 0) = 1 THEN source.email ELSE NULL END),
        target.deleted_at = COALESCE(target.deleted_at, source.deleted_at),
        target.deleted_by = COALESCE(target.deleted_by, source.deleted_by),
        target.status = CASE
            WHEN COALESCE(target.deleted, 0) = 1 OR COALESCE(source.deleted, 0) = 1 THEN 'SUSPENDED'
            WHEN target.status = 'INACTIVE' AND COALESCE(source.enabled, 1) = 1 THEN 'ACTIVE'
            ELSE target.status END
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (full_name, email, password_hash, student, currently_studying, student_id, graduation_date,
                role, status, enabled, deleted, deleted_email, deleted_at, deleted_by, created_at,
                title, student_code, faculty_id)
        VALUES (
            LEFT(COALESCE(NULLIF(LTRIM(RTRIM(source.display_name)), ''), source.email), 100),
            CASE WHEN COALESCE(source.deleted, 0) = 1
                 THEN CONCAT('deleted-legacy-', source.id, '-', LOWER(CONVERT(varchar(36), NEWID())), '@invalid.local')
                 ELSE source.email END,
            NULLIF(source.password_hash, ''),
            CASE WHEN CHARINDEX(',STUDENT,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0 THEN 1 ELSE 0 END,
            CASE WHEN CHARINDEX(',STUDENT,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0
                   AND NULLIF(LTRIM(RTRIM(source.student_code)), '') IS NOT NULL THEN 1 ELSE 0 END,
            CASE WHEN CHARINDEX(',STUDENT,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0
                 THEN NULLIF(source.student_code, '') ELSE NULL END,
            NULL,
            CASE WHEN CHARINDEX(',ADMIN,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0 THEN 'ADMIN'
                 WHEN CHARINDEX(',CONSULTANT,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0
                   OR CHARINDEX(',ADVISOR,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0
                   OR CHARINDEX(',LECTURER,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0
                   OR CHARINDEX(',STAFF,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0
                   OR CHARINDEX(',MANAGER,', ',' + REPLACE(UPPER(COALESCE(source.roles, '')), ' ', '') + ',') > 0 THEN 'CONSULTANT'
                 ELSE 'USER' END,
            CASE WHEN COALESCE(source.deleted, 0) = 1 THEN 'SUSPENDED'
                 WHEN COALESCE(source.enabled, 1) = 1 THEN 'ACTIVE' ELSE 'INACTIVE' END,
            COALESCE(source.enabled, 1),
            COALESCE(source.deleted, 0),
            COALESCE(source.deleted_email, CASE WHEN COALESCE(source.deleted, 0) = 1 THEN source.email ELSE NULL END),
            source.deleted_at,
            source.deleted_by,
            SYSDATETIME(),
            LEFT(source.title, 160),
            LEFT(source.student_code, 30),
            source.faculty_id
        )
    OUTPUT source.id, inserted.id INTO @UserIdMap(legacy_user_id, user_id);

    UPDATE target SET email = CONCAT('deleted-merge-', target.id, '-', LOWER(CONVERT(varchar(36), NEWID())), '@invalid.local')
    FROM dbo.users AS target
    INNER JOIN @UserIdMap AS mapping ON mapping.user_id = target.id
    INNER JOIN dbo.user_accounts AS source ON source.id = mapping.legacy_user_id
    WHERE COALESCE(target.deleted, 0) = 1
      AND LOWER(LTRIM(RTRIM(target.email))) = LOWER(LTRIM(RTRIM(source.email)));

    INSERT INTO dbo.user_consultant_titles(user_id, title)
    SELECT DISTINCT mapping.user_id,
           CASE UPPER(LTRIM(RTRIM(role_value.value))) WHEN 'MANAGER' THEN 'TEAM_LEADER'
                ELSE UPPER(LTRIM(RTRIM(role_value.value))) END
    FROM dbo.user_accounts AS source
    INNER JOIN @UserIdMap AS mapping ON mapping.legacy_user_id = source.id
    CROSS APPLY STRING_SPLIT(COALESCE(source.roles, ''), ',') AS role_value
    WHERE UPPER(LTRIM(RTRIM(role_value.value))) IN ('STUDENT', 'ADVISOR', 'LECTURER', 'STAFF', 'MANAGER', 'TEAM_LEADER')
      AND NOT EXISTS (
          SELECT 1 FROM dbo.user_consultant_titles AS existing
          WHERE existing.user_id = mapping.user_id
            AND existing.title = CASE UPPER(LTRIM(RTRIM(role_value.value))) WHEN 'MANAGER' THEN 'TEAM_LEADER'
                                  ELSE UPPER(LTRIM(RTRIM(role_value.value))) END
      );

    UPDATE users SET role = CASE WHEN role IN ('ADVISOR', 'LECTURER', 'STAFF', 'TEAM_LEADER') THEN 'CONSULTANT'
                                 WHEN role = 'STUDENT' THEN 'USER' ELSE role END;

    UPDATE target SET role = 'CONSULTANT'
    FROM dbo.users AS target
    WHERE target.role = 'USER'
      AND EXISTS (SELECT 1 FROM dbo.user_consultant_titles AS title
                  WHERE title.user_id = target.id AND title.title <> 'STUDENT');

    IF OBJECT_ID(N'dbo.user_roles', N'U') IS NOT NULL
    BEGIN
        EXEC sys.sp_executesql N'
            INSERT INTO dbo.user_consultant_titles(user_id, title)
            SELECT DISTINCT legacy.user_id,
                   CASE legacy.role WHEN ''MANAGER'' THEN ''TEAM_LEADER'' ELSE legacy.role END
            FROM dbo.user_roles AS legacy
            WHERE legacy.role IN (''STUDENT'', ''ADVISOR'', ''LECTURER'', ''STAFF'', ''TEAM_LEADER'', ''MANAGER'')
              AND NOT EXISTS (
                  SELECT 1 FROM dbo.user_consultant_titles AS existing
                  WHERE existing.user_id = legacy.user_id
                    AND existing.title = CASE legacy.role WHEN ''MANAGER'' THEN ''TEAM_LEADER'' ELSE legacy.role END
              );
            UPDATE target SET student = 1
            FROM dbo.users AS target
            WHERE EXISTS (SELECT 1 FROM dbo.user_roles AS legacy WHERE legacy.user_id = target.id AND legacy.role = ''STUDENT'');
            UPDATE users SET role = CASE WHEN role IN (''ADVISOR'', ''LECTURER'', ''STAFF'', ''TEAM_LEADER'') THEN ''CONSULTANT''
                                         WHEN role = ''STUDENT'' THEN ''USER'' ELSE role END;
            UPDATE target SET role = ''CONSULTANT''
            FROM dbo.users AS target
            WHERE target.role = ''USER''
              AND EXISTS (SELECT 1 FROM dbo.user_consultant_titles AS title
                          WHERE title.user_id = target.id AND title.title <> ''STUDENT'');
        ';
    END;

    DECLARE @dropForeignKeys nvarchar(max) = N'';
    SELECT @dropForeignKeys = @dropForeignKeys
        + N'ALTER TABLE ' + QUOTENAME(schema_name(t.schema_id)) + N'.' + QUOTENAME(t.name)
        + N' DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';'
    FROM sys.foreign_keys AS fk
    INNER JOIN sys.tables AS t ON t.object_id = fk.parent_object_id
    WHERE fk.referenced_object_id = OBJECT_ID(N'dbo.user_accounts');
    IF @dropForeignKeys <> N'' EXEC sys.sp_executesql @dropForeignKeys;

    IF OBJECT_ID(N'dbo.user_faculty_topics', N'U') IS NOT NULL
    BEGIN
        INSERT INTO dbo.user_assigned_topics(user_id, topic_id)
        SELECT DISTINCT mapping.user_id, links.topic_id
        FROM dbo.user_faculty_topics AS links
        INNER JOIN @UserIdMap AS mapping ON mapping.legacy_user_id = links.user_id
        WHERE NOT EXISTS (
            SELECT 1 FROM dbo.user_assigned_topics AS existing
            WHERE existing.user_id = mapping.user_id AND existing.topic_id = links.topic_id
        );
    END;

    UPDATE questions SET asker_id = mapping.user_id
    FROM dbo.questions AS questions
    INNER JOIN @UserIdMap AS mapping ON mapping.legacy_user_id = questions.asker_id;

    UPDATE answers SET author_id = mapping.user_id
    FROM dbo.answers AS answers
    INNER JOIN @UserIdMap AS mapping ON mapping.legacy_user_id = answers.author_id;

    UPDATE transfers SET transferred_by_id = mapping.user_id
    FROM dbo.question_transfers AS transfers
    INNER JOIN @UserIdMap AS mapping ON mapping.legacy_user_id = transfers.transferred_by_id;

    IF OBJECT_ID(N'dbo.user_faculty_topics', N'U') IS NOT NULL
        EXEC sys.sp_rename N'dbo.user_faculty_topics', N'user_faculty_topics_merged_archive';
    IF OBJECT_ID(N'dbo.user_roles', N'U') IS NOT NULL
        EXEC sys.sp_rename N'dbo.user_roles', N'user_roles_merged_archive';
    EXEC sys.sp_rename N'dbo.user_accounts', N'user_accounts_merged_archive';

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_questions_users_asker')
        ALTER TABLE dbo.questions ADD CONSTRAINT FK_questions_users_asker FOREIGN KEY (asker_id) REFERENCES dbo.users(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_answers_users_author')
        ALTER TABLE dbo.answers ADD CONSTRAINT FK_answers_users_author FOREIGN KEY (author_id) REFERENCES dbo.users(id);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_question_transfers_users_transferred_by')
        ALTER TABLE dbo.question_transfers ADD CONSTRAINT FK_question_transfers_users_transferred_by FOREIGN KEY (transferred_by_id) REFERENCES dbo.users(id);

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_users_faculties_faculty')
        ALTER TABLE dbo.users ADD CONSTRAINT FK_users_faculties_faculty FOREIGN KEY (faculty_id) REFERENCES dbo.faculties(id);

    COMMIT TRANSACTION;
    PRINT 'UserAccount data, consultant titles, topic assignments and question relationships were merged into dbo.users.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
