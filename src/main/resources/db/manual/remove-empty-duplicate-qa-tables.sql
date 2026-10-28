/*
 * Keep dbo.questions and dbo.answers. Remove the legacy qaute_* tables only
 * when they are empty. The current answer_review table may still have a stale
 * foreign key to qaute_answers, which prevents reviews from being saved.
 */
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.questions', N'U') IS NULL OR OBJECT_ID(N'dbo.answers', N'U') IS NULL
        THROW 51300, 'The active dbo.questions/dbo.answers tables are missing.', 1;

    IF (SELECT COUNT_BIG(*) FROM dbo.questions) = 0 OR (SELECT COUNT_BIG(*) FROM dbo.answers) = 0
        THROW 51301, 'The active question/answer tables are empty; refusing to choose a table set.', 1;

    IF OBJECT_ID(N'dbo.qaute_questions', N'U') IS NOT NULL
       AND EXISTS (SELECT 1 FROM dbo.qaute_questions)
        THROW 51302, 'dbo.qaute_questions contains data; migrate it before removing duplicate tables.', 1;

    IF OBJECT_ID(N'dbo.qaute_answers', N'U') IS NOT NULL
       AND EXISTS (SELECT 1 FROM dbo.qaute_answers)
        THROW 51303, 'dbo.qaute_answers contains data; migrate it before removing duplicate tables.', 1;

    DECLARE @schemaName sysname;
    DECLARE @tableName sysname;
    DECLARE @constraintName sysname;
    DECLARE @dropStatement nvarchar(776);
    DECLARE stale_fk_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT parent_schema.name, parent_table.name, fk.name
        FROM sys.foreign_keys AS fk
        JOIN sys.tables AS parent_table ON parent_table.object_id = fk.parent_object_id
        JOIN sys.schemas AS parent_schema ON parent_schema.schema_id = parent_table.schema_id
        WHERE fk.referenced_object_id IN (
            OBJECT_ID(N'dbo.qaute_answers', N'U'),
            OBJECT_ID(N'dbo.qaute_questions', N'U'))
          AND fk.parent_object_id NOT IN (
            OBJECT_ID(N'dbo.qaute_answers', N'U'),
            OBJECT_ID(N'dbo.qaute_questions', N'U'));

    OPEN stale_fk_cursor;
    FETCH NEXT FROM stale_fk_cursor INTO @schemaName, @tableName, @constraintName;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @dropStatement = N'ALTER TABLE ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName)
            + N' DROP CONSTRAINT ' + QUOTENAME(@constraintName);
        EXEC sys.sp_executesql @dropStatement;
        FETCH NEXT FROM stale_fk_cursor INTO @schemaName, @tableName, @constraintName;
    END;
    CLOSE stale_fk_cursor;
    DEALLOCATE stale_fk_cursor;

    IF OBJECT_ID(N'dbo.qaute_answers', N'U') IS NOT NULL DROP TABLE dbo.qaute_answers;
    IF OBJECT_ID(N'dbo.qaute_questions', N'U') IS NOT NULL DROP TABLE dbo.qaute_questions;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF CURSOR_STATUS('local', 'stale_fk_cursor') >= 0 CLOSE stale_fk_cursor;
    IF CURSOR_STATUS('local', 'stale_fk_cursor') > -3 DEALLOCATE stale_fk_cursor;
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
