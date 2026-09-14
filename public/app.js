'use strict';

let chapters = [];
let sortKey = null;
let ascending = true;
const tbody = document.querySelector('#chapters');
const status = document.querySelector('#status');

function render() {
  tbody.replaceChildren();
  chapters.forEach(chapter => {
    const row = document.createElement('tr');
    const number = document.createElement('th');
    number.scope = 'row';
    number.className = 'chapter-number';
    number.textContent = `第${chapter.chapter}章`;
    row.append(number);

    const content = document.createElement('td');
    content.className = 'chapter-content';
    const title = document.createElement('strong');
    title.textContent = chapter.title;
    const topics = document.createElement('ul');
    topics.className = 'topic-list';
    const cell = document.createElement('td');
    cell.className = 'understanding-cell';
    const average = document.createElement('output');
    average.setAttribute('aria-label', `第${chapter.chapter}章の平均理解度`);
    const coverage = document.createElement('p');
    coverage.className = 'coverage';
    const progress = document.createElement('progress');
    progress.max = 100;
    progress.setAttribute('aria-label', `第${chapter.chapter}章の平均理解度`);

    function updateSummary() {
      const entered = chapter.topicUnderstanding.filter(value => value !== null);
      chapter.understanding = entered.length
        ? entered.reduce((sum, value) => sum + value, 0) / entered.length : null;
      average.textContent = chapter.understanding === null ? '未入力' : `${chapter.understanding.toFixed(1)}％`;
      coverage.textContent = `${entered.length} / ${chapter.topics.length}項目入力済み`;
      progress.value = chapter.understanding ?? 0;
      progress.hidden = chapter.understanding === null;
    }

    chapter.topics.forEach((topic, index) => {
      const item = document.createElement('li');
      const label = document.createElement('label');
      label.textContent = topic;
      const input = document.createElement('input');
      input.id = `chapter-${chapter.chapter}-topic-${index + 1}`;
      label.htmlFor = input.id;
      input.type = 'number';
      input.setAttribute('aria-label', `第${chapter.chapter}章・項目${index + 1}の理解度：${topic}`);
      input.setAttribute('aria-describedby', 'understanding-help');
      input.value = chapter.topicUnderstanding[index] ?? '';
      input.placeholder = '未入力';
      input.min = '0';
      input.max = '100';
      input.step = 'any';
      input.inputMode = 'decimal';
      const field = document.createElement('div');
      field.className = 'topic-rating';
      const unit = document.createElement('span');
      unit.textContent = '％';
      field.append(input, unit);
      input.addEventListener('blur', () => {
        const raw = input.value.trim();
        const value = raw === '' ? null : Number(raw);
        if (input.validity.badInput || (value !== null && (!Number.isFinite(value) || value < 0 || value > 100))) {
          status.textContent = '理解度は0〜100の数値で入力してください。変更を取り消しました。';
          input.value = chapter.topicUnderstanding[index] ?? '';
          return;
        }
        chapter.topicUnderstanding[index] = value;
        updateSummary();
        sortKey = null;
        updateSortLabels();
        updateCount();
        status.textContent = `第${chapter.chapter}章・項目${index + 1}の理解度を${value === null ? '未入力に戻しました' : value + '％に変更しました'}（この画面のみ・保存なし）。`;
      });
      input.addEventListener('keydown', event => {
        if (event.key === 'Enter') input.blur();
        if (event.key === 'Escape') {
          input.value = chapter.topicUnderstanding[index] ?? '';
          input.blur();
        }
      });
      item.append(label, field);
      topics.append(item);
    });
    updateSummary();
    cell.append(average, coverage, progress);
    content.append(title, topics);
    row.append(content, cell);
    tbody.append(row);
  });
  updateCount();
}

function updateCount() {
  const values = chapters.flatMap(chapter => chapter.topicUnderstanding);
  const entered = values.filter(value => value !== null).length;
  document.querySelector('#count').textContent = `全${chapters.length}章 · 理解度入力済み ${entered} / ${values.length}項目`;
}

function updateSortLabels() {
  for (const key of ['chapter', 'understanding']) {
    const button = document.querySelector(`#sort-${key}`);
    button.parentElement.setAttribute('aria-sort', sortKey === key
      ? (ascending ? 'ascending' : 'descending') : 'none');
    button.querySelector('span').textContent = sortKey === key ? (ascending ? '↑' : '↓') : '↕';
  }
}

for (const key of ['chapter', 'understanding']) {
  document.querySelector(`#sort-${key}`).addEventListener('click', () => {
    ascending = sortKey === key ? !ascending : true;
    sortKey = key;
    chapters.sort((a, b) => {
      // 未入力は昇順・降順とも末尾へ。0％とは区別する。
      if (key === 'understanding') {
        if (a[key] === null && b[key] !== null) return 1;
        if (a[key] !== null && b[key] === null) return -1;
      }
      return (a[key] - b[key]) * (ascending ? 1 : -1) || a.chapter - b.chapter;
    });
    render();
    updateSortLabels();
    status.textContent = `${key === 'understanding' ? '理解度' : '章番号'}の${ascending ? '昇順' : '降順'}に並べ替えました。`;
  });
}

async function load() {
  const retry = document.querySelector('#retry');
  retry.hidden = true;
  status.textContent = '学習内容を読み込んでいます。';
  try {
    const response = await fetch('/api/chapters');
    if (!response.ok) throw new Error('Failed to load chapters');
    chapters = (await response.json()).map(chapter => ({
      ...chapter, topicUnderstanding: chapter.topics.map(() => null)
    }));
    sortKey = null;
    render();
    updateSortLabels();
    status.textContent = '理解度を入力したらEnterキー、または別のセルを選んで確定します。';
  } catch (error) {
    document.querySelector('#count').textContent = '読み込みエラー';
    status.textContent = '学習内容を読み込めませんでした。時間をおいて再読み込みしてください。';
    retry.hidden = false;
  }
}
document.querySelector('#retry').addEventListener('click', load);
load();
