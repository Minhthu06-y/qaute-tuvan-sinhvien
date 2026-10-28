/*
 * Configure the existing local QAUTE test accounts and department scope.
 * The stored password value below is a BCrypt hash for the shared local test password.
 */
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @TestPasswordHash varchar(100) = '$2a$10$hOPmYDHiKPGGxj38S5DLuuPeCxX2cxO767l5phWWMeelc8wazik62';
DECLARE @Consultants TABLE (
    email nvarchar(150) NOT NULL,
    faculty_short_name nvarchar(30) NOT NULL,
    consultant_title varchar(20) NOT NULL,
    topic_name nvarchar(240) NOT NULL
);
INSERT INTO @Consultants(email, faculty_short_name, consultant_title, topic_name)
VALUES
    (N'canbo@qaute.edu.vn', N'PĐT', 'STAFF', N'Học vụ · Học tập'),
    (N'giangvien@qaute.edu.vn', N'CNTT', 'LECTURER', N'Chương trình đào tạo · Ngành học'),
    (N'tuvanvien@qaute.edu.vn', N'TTDVSV', 'ADVISOR', N'Tư vấn học đường · Tâm lý');

BEGIN TRY
    BEGIN TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = N'manager@qaute.edu.vn' AND role = N'MANAGER')
        THROW 51200, 'Expected local manager account was not found.', 1;

    IF EXISTS (
        SELECT 1
        FROM @Consultants c
        LEFT JOIN dbo.users u ON u.email = c.email AND u.role = N'CONSULTANT'
        LEFT JOIN dbo.faculties f ON f.short_name = c.faculty_short_name
        LEFT JOIN dbo.department d ON d.name = f.name
        LEFT JOIN dbo.faculty_topics t ON t.faculty_id = f.id AND t.name = c.topic_name
        LEFT JOIN dbo.advisor_profile ap ON ap.user_id = u.id
        WHERE u.id IS NULL OR f.id IS NULL OR d.id IS NULL OR t.id IS NULL OR ap.id IS NULL
    )
        THROW 51201, 'A consultant, profile, matching faculty/department, or topic is missing.', 1;

    /* Restore the faculty-equivalent department on questions created before the merge. */
    UPDATE q
       SET department_id = d.id
    FROM dbo.questions q
    JOIN dbo.faculties f ON f.id = q.faculty_id
    JOIN dbo.department d ON d.name = f.name
    WHERE q.department_id IS NULL;

    UPDATE u
       SET faculty_id = f.id
    FROM dbo.users u
    JOIN @Consultants c ON c.email = u.email
    JOIN dbo.faculties f ON f.short_name = c.faculty_short_name;

    /* The old database CHECK constraint predates the ADVISOR consultant title. */
    DECLARE @dropAdvisorTypeChecks nvarchar(max) = N'';
    SELECT @dropAdvisorTypeChecks = @dropAdvisorTypeChecks
        + N'ALTER TABLE dbo.advisor_profile DROP CONSTRAINT ' + QUOTENAME(name) + N';'
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.advisor_profile')
      AND definition LIKE N'%advisor_type%';
    IF @dropAdvisorTypeChecks <> N''
        EXEC sys.sp_executesql @dropAdvisorTypeChecks;
    ALTER TABLE dbo.advisor_profile WITH CHECK ADD CONSTRAINT CK_advisor_profile_advisor_type
        CHECK (advisor_type IN ('STUDENT', 'ADVISOR', 'LECTURER', 'STAFF', 'TEAM_LEADER'));

    UPDATE ap
       SET department_id = d.id,
           advisor_type = c.consultant_title
    FROM dbo.advisor_profile ap
    JOIN dbo.users u ON u.id = ap.user_id
    JOIN @Consultants c ON c.email = u.email
    JOIN dbo.faculties f ON f.short_name = c.faculty_short_name
    JOIN dbo.department d ON d.name = f.name;

    DELETE titles
    FROM dbo.user_consultant_titles titles
    JOIN dbo.users u ON u.id = titles.user_id
    JOIN @Consultants c ON c.email = u.email;

    INSERT INTO dbo.user_consultant_titles(user_id, title)
    SELECT u.id, c.consultant_title
    FROM dbo.users u
    JOIN @Consultants c ON c.email = u.email;

    DELETE assignments
    FROM dbo.user_assigned_topics assignments
    JOIN dbo.users u ON u.id = assignments.user_id
    JOIN @Consultants c ON c.email = u.email;

    INSERT INTO dbo.user_assigned_topics(user_id, topic_id)
    SELECT u.id, t.id
    FROM dbo.users u
    JOIN @Consultants c ON c.email = u.email
    JOIN dbo.faculties f ON f.short_name = c.faculty_short_name
    JOIN dbo.faculty_topics t ON t.faculty_id = f.id AND t.name = c.topic_name;

    /* All application accounts use the requested BCrypt-encoded test password. */
    UPDATE dbo.users
       SET password_hash = @TestPasswordHash,
           password = @TestPasswordHash;

    DECLARE @ManagerId bigint =
        (SELECT id FROM dbo.users WHERE email = N'manager@qaute.edu.vn' AND role = N'MANAGER');

    /* Give the test manager scope over current question departments and consultant units. */
    INSERT INTO dbo.manager_departments(user_id, department_id)
    SELECT DISTINCT @ManagerId, scope.department_id
    FROM (
        SELECT q.department_id
        FROM dbo.questions q
        WHERE q.department_id IS NOT NULL
        UNION
        SELECT d.id
        FROM @Consultants c
        JOIN dbo.faculties f ON f.short_name = c.faculty_short_name
        JOIN dbo.department d ON d.name = f.name
    ) scope
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.manager_departments existing
        WHERE existing.user_id = @ManagerId AND existing.department_id = scope.department_id
    );

    COMMIT TRANSACTION;

    SELECT u.full_name, u.email, u.role, u.status, f.name AS faculty,
           ap.advisor_type, d.name AS department, t.name AS assigned_topic
    FROM dbo.users u
    LEFT JOIN dbo.faculties f ON f.id = u.faculty_id
    LEFT JOIN dbo.advisor_profile ap ON ap.user_id = u.id
    LEFT JOIN dbo.department d ON d.id = ap.department_id
    LEFT JOIN dbo.user_assigned_topics ut ON ut.user_id = u.id
    LEFT JOIN dbo.faculty_topics t ON t.id = ut.topic_id
    WHERE u.role IN (N'MANAGER', N'CONSULTANT')
    ORDER BY u.role, u.email, t.name;

    SELECT COUNT(DISTINCT q.id) AS manager_visible_questions,
           COUNT(DISTINCT a.id) AS manager_visible_answers,
           COUNT(DISTINCT md.department_id) AS manager_department_count
    FROM dbo.users manager
    JOIN dbo.manager_departments md ON md.user_id = manager.id
    LEFT JOIN dbo.questions q ON q.department_id = md.department_id
    LEFT JOIN dbo.answers a ON a.question_id = q.id
    WHERE manager.email = N'manager@qaute.edu.vn';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

