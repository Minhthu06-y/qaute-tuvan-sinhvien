document.querySelectorAll('[data-topic-selector]').forEach(topicPicker => {
  const facultySelect = document.querySelector(topicPicker.dataset.facultySelect);
  if (!facultySelect) return;

  const isSelect = topicPicker instanceof HTMLSelectElement;

  if (isSelect) {
    const options = [...topicPicker.querySelectorAll('option[data-faculty-id]')];
    const updateSelect = () => {
      const facultyId = facultySelect.value;
      let hasTopics = false;

      options.forEach(option => {
        const matches = facultyId && option.dataset.facultyId === facultyId;
        option.hidden = !matches;
        option.disabled = !matches;
        if (matches) hasTopics = true;
      });

      if (!facultyId || !hasTopics || topicPicker.selectedOptions[0]?.disabled) topicPicker.value = '';
      topicPicker.disabled = !facultyId || !hasTopics;
    };

    facultySelect.addEventListener('change', updateSelect);
    updateSelect();
    return;
  }

  const choices = [...topicPicker.querySelectorAll('.topic-option[data-faculty-id]')];
  const emptyHint = topicPicker.querySelector('[data-topic-empty]');
  const updateChoices = () => {
    const facultyId = facultySelect.value;
    let hasTopics = false;

    choices.forEach(choice => {
      const matches = facultyId && choice.dataset.facultyId === facultyId;
      const input = choice.querySelector('input[name="topicId"]');
      choice.hidden = !matches;
      input.disabled = !matches;
      if (!matches && input.checked) input.checked = false;
      if (matches) hasTopics = true;
    });

    emptyHint.hidden = Boolean(facultyId && hasTopics);
    emptyHint.textContent = facultyId && !hasTopics
      ? 'Đơn vị này chưa có chuyên đề.'
      : 'Chọn khoa hoặc đơn vị để xem các chuyên đề.';
  };

  facultySelect.addEventListener('change', updateChoices);
  updateChoices();
});
