/*
 * One-time SQL Server migration for consolidating the parallel question/answer stores.
 * The application now writes only dbo.questions and dbo.answers.
 * Back up the QAUTE database before running this script.
 */
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.qaute_questions', N'U') IS NULL
   AND OBJECT_ID(N'dbo.qaute_answers', N'U') IS NULL
BEGIN
    PRINT 'The legacy qaute_* tables are absent; no migration is required.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.qaute_questions', N'U') IS NULL
   OR OBJECT_ID(N'dbo.qaute_answers', N'U') IS NULL
    THROW 51100, 'Both dbo.qaute_questions and dbo.qaute_answers must exist to migrate the legacy store.', 1;

IF OBJECT_ID(N'dbo.questions', N'U') IS NULL OR OBJECT_ID(N'dbo.answers', N'U') IS NULL
    THROW 51101, 'The canonical dbo.questions and dbo.answers tables must exist before migration.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH(N'dbo.questions', N'department_id') IS NULL
        ALTER TABLE dbo.questions ADD department_id bigint NULL;
    IF COL_LENGTH(N'dbo.questions', N'category') IS NULL
        ALTER TABLE dbo.questions ADD category nvarchar(100) NULL;
    IF COL_LENGTH(N'dbo.questions', N'audience') IS NULL
        ALTER TABLE dbo.questions ADD audience varchar(40) NULL;

    ALTER TABLE dbo.questions ALTER COLUMN faculty_id bigint NULL;
    ALTER TABLE dbo.questions ALTER COLUMN subject nvarchar(240) NOT NULL;
    ALTER TABLE dbo.questions ALTER COLUMN body nvarchar(max) NOT NULL;
    ALTER TABLE dbo.answers ALTER COLUMN body nvarchar(max) NOT NULL;
    ALTER TABLE dbo.answers ALTER COLUMN review_reason nvarchar(max) NULL;

    IF NOT EXISTS (SELECT 1 FROM dbo.faculties WHERE name = N'Chưa phân khoa')
        INSERT INTO dbo.faculties (name, short_name) VALUES (N'Chưa phân khoa', N'');
    DECLARE @FallbackFacultyId bigint = (SELECT TOP (1) id FROM dbo.faculties WHERE name = N'Chưa phân khoa');

    INSERT INTO dbo.department (name)
    SELECT DISTINCT faculty.name
    FROM dbo.faculties AS faculty
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.department AS department
        WHERE LOWER(LTRIM(RTRIM(department.name))) = LOWER(LTRIM(RTRIM(faculty.name)))
    );

    UPDATE question SET department_id = department.id
    FROM dbo.questions AS question
    INNER JOIN dbo.faculties AS faculty ON faculty.id = question.faculty_id
    INNER JOIN dbo.department AS department
        ON LOWER(LTRIM(RTRIM(department.name))) = LOWER(LTRIM(RTRIM(faculty.name)))
    WHERE question.department_id IS NULL;

    IF EXISTS (
        SELECT 1 FROM dbo.qaute_questions
        WHERE COALESCE(asker_id, user_id) IS NULL
    )
        THROW 51102, 'A legacy question has no owner; resolve it before migration.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.qaute_answers
        WHERE COALESCE(advisor_id, user_id) IS NULL
    )
        THROW 51103, 'A legacy answer has no author; resolve it before migration.', 1;

    DECLARE @dropSql nvarchar(max) = N'';
    SELECT @dropSql = @dropSql
        + N'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(fk.parent_object_id)) + N'.'
        + QUOTENAME(OBJECT_NAME(fk.parent_object_id)) + N' DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';'
    FROM sys.foreign_keys AS fk
    WHERE fk.referenced_object_id IN (OBJECT_ID(N'dbo.qaute_answers'), OBJECT_ID(N'dbo.qaute_questions'));
    IF @dropSql <> N'' EXEC sys.sp_executesql @dropSql;

    DECLARE @QuestionIdMap table (legacy_id bigint NOT NULL PRIMARY KEY, merged_id bigint NOT NULL);

    MERGE dbo.questions AS target
    USING (
        SELECT q.id AS legacy_id, COALESCE(q.asker_id, q.user_id) AS asker_id,
               COALESCE(source_department.id, asker_department.id) AS department_id,
               COALESCE(f_by_department.id, asker.faculty_id, @FallbackFacultyId) AS faculty_id,
               q.title, q.content, q.category, q.audience, q.public_visible, q.public_question,
               q.status, q.created_at
        FROM dbo.qaute_questions AS q
        LEFT JOIN dbo.department AS source_department ON source_department.id = q.department_id
        LEFT JOIN dbo.users AS asker ON asker.id = COALESCE(q.asker_id, q.user_id)
        LEFT JOIN dbo.faculties AS asker_faculty ON asker_faculty.id = asker.faculty_id
        LEFT JOIN dbo.department AS asker_department
            ON LOWER(LTRIM(RTRIM(asker_department.name))) = LOWER(LTRIM(RTRIM(asker_faculty.name)))
        LEFT JOIN dbo.faculties AS f_by_department
            ON LOWER(LTRIM(RTRIM(f_by_department.name))) = LOWER(LTRIM(RTRIM(source_department.name)))
    ) AS source
    ON 1 = 0
    WHEN NOT MATCHED THEN
        INSERT (asker_id, faculty_id, topic_id, department_id, subject, body, category, audience,
                created_at, status, is_public, locked, reply_notification, attachment_url)
        VALUES (source.asker_id, source.faculty_id, NULL, source.department_id,
                LEFT(COALESCE(NULLIF(LTRIM(RTRIM(source.title)), N''), N'(không có tiêu đề)'), 240),
                COALESCE(source.content, N''), source.category, source.audience, source.created_at,
                CASE WHEN source.status IS NULL OR source.status IN ('OPEN', 'UNANSWERED')
                     THEN 'UNANSWERED' ELSE source.status END,
                CASE WHEN COALESCE(source.public_visible, 0) = 1 OR COALESCE(source.public_question, 0) = 1 THEN 1 ELSE 0 END,
                0, 0, NULL)
    OUTPUT source.legacy_id, inserted.id INTO @QuestionIdMap(legacy_id, merged_id);

    UPDATE q SET department_id = d.id
    FROM dbo.questions AS q
    INNER JOIN dbo.faculties AS f ON f.id = q.faculty_id
    INNER JOIN dbo.department AS d
        ON LOWER(LTRIM(RTRIM(d.name))) = LOWER(LTRIM(RTRIM(f.name)))
    WHERE q.department_id IS NULL;

    DECLARE @AnswerIdMap table (legacy_id bigint NOT NULL PRIMARY KEY, merged_id bigint NOT NULL);

    MERGE dbo.answers AS target
    USING (
        SELECT a.id AS legacy_id, qmap.merged_id AS question_id,
               COALESCE(a.advisor_id, a.user_id) AS author_id, a.content, a.created_at,
               CASE
                   WHEN a.approval_status IS NOT NULL THEN a.approval_status
                   WHEN COALESCE(a.need_approval, 0) = 1 THEN 'PENDING'
                   WHEN COALESCE(a.published, 0) = 1 OR COALESCE(a.approved, 0) = 1 THEN 'APPROVED'
                   ELSE 'NOT_REQUIRED'
               END AS approval_status
        FROM dbo.qaute_answers AS a
        INNER JOIN @QuestionIdMap AS qmap ON qmap.legacy_id = a.question_id
    ) AS source
    ON 1 = 0
    WHEN NOT MATCHED THEN
        INSERT (question_id, author_id, body, created_at, approval_status, review_reason)
        VALUES (source.question_id, source.author_id, COALESCE(source.content, N''), source.created_at,
                source.approval_status, NULL)
    OUTPUT source.legacy_id, inserted.id INTO @AnswerIdMap(legacy_id, merged_id);

    IF OBJECT_ID(N'dbo.answer_review', N'U') IS NOT NULL
    BEGIN
        IF EXISTS (
            SELECT 1 FROM dbo.answer_review AS review
            LEFT JOIN @AnswerIdMap AS mapping ON mapping.legacy_id = review.answer_id
            WHERE mapping.legacy_id IS NULL
        )
            THROW 51104, 'An answer review does not map to a legacy answer; resolve it before migration.', 1;

        UPDATE review SET answer_id = mapping.merged_id
        FROM dbo.answer_review AS review
        INNER JOIN @AnswerIdMap AS mapping ON mapping.legacy_id = review.answer_id;

        IF NOT EXISTS (
            SELECT 1 FROM sys.foreign_keys
            WHERE parent_object_id = OBJECT_ID(N'dbo.answer_review')
              AND referenced_object_id = OBJECT_ID(N'dbo.answers')
        )
            ALTER TABLE dbo.answer_review ADD CONSTRAINT FK_answer_review_answers
                FOREIGN KEY (answer_id) REFERENCES dbo.answers(id);
    END;

    UPDATE dbo.questions SET status = 'UNANSWERED' WHERE status IS NULL OR status = 'OPEN';

    DROP TABLE dbo.qaute_answers;
    DROP TABLE dbo.qaute_questions;

    COMMIT TRANSACTION;
    PRINT 'Legacy questions and answers were merged into dbo.questions and dbo.answers.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
