const MAP = {
  she: '她',
  he: '他',
  ta: 'TA',
};

export function pronounLabel(code) {
  return MAP[code] || 'TA';
}
