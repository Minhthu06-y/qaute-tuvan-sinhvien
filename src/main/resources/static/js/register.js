document.addEventListener("DOMContentLoaded", () => {
    const student = document.getElementById("student");
    const currentlyStudying =
        document.getElementById("currentlyStudying");

    const studentFields = document.getElementById("studentFields");
    const studentIdGroup = document.getElementById("studentIdGroup");
    const graduationGroup = document.getElementById("graduationGroup");

    function updateFields() {
        studentFields.hidden = !student.checked;

        studentIdGroup.hidden =
            !student.checked || !currentlyStudying.checked;

        graduationGroup.hidden =
            !student.checked || currentlyStudying.checked;
    }

    student.addEventListener("change", updateFields);
    currentlyStudying.addEventListener("change", updateFields);

    updateFields();
});