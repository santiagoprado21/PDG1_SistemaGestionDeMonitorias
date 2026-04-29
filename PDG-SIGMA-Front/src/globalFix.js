if (typeof window !== 'undefined' && typeof window.isNumber === 'undefined') {
    window.isNumber = function (value) {
      return typeof value === 'number' && !isNaN(value);
    };
  }

const PERIOD_REGEX = /^\d{4}-[12]$/;

export const getCurrentAcademicPeriod = () => {
  const now = new Date();
  const semester = now.getMonth() <= 5 ? 1 : 2;
  return `${now.getFullYear()}-${semester}`;
};

export const generateAcademicPeriodOptions = (year = new Date().getFullYear()) => [
  `${year}-1`,
  `${year}-2`
];

export const isAcademicPeriodFormat = (value) => {
  const normalized = (value || '').trim();
  return PERIOD_REGEX.test(normalized);
};

export const isSelectableAcademicPeriod = (value, year = new Date().getFullYear()) => {
  const normalized = (value || '').trim();
  return PERIOD_REGEX.test(normalized) && Number(normalized.slice(0, 4)) === year;
};
  