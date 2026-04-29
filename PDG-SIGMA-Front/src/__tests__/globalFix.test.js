describe('globalFix', () => {
  afterEach(() => {
    jest.resetModules();
    delete window.isNumber;
  });

  test('define window.isNumber cuando no existe', () => {
    delete window.isNumber;

    jest.isolateModules(() => {
      require('../globalFix');
    });

    expect(window.isNumber).toBeDefined();
    expect(window.isNumber(10)).toBe(true);
    expect(window.isNumber(NaN)).toBe(false);
    expect(window.isNumber('10')).toBe(false);
  });

  test('expone helpers para periodos academicos', () => {
    const helpers = require('../globalFix');

    expect(helpers.generateAcademicPeriodOptions(2026)).toEqual(['2026-1', '2026-2']);
    expect(helpers.isAcademicPeriodFormat('2026-1')).toBe(true);
    expect(helpers.isAcademicPeriodFormat('2026-3')).toBe(false);
    expect(helpers.isSelectableAcademicPeriod('2026-1', 2026)).toBe(true);
    expect(helpers.isSelectableAcademicPeriod('2025-1', 2026)).toBe(false);
  });

  test('no sobrescribe window.isNumber si ya existe', () => {
    const original = jest.fn(() => 'custom');
    window.isNumber = original;

    jest.isolateModules(() => {
      require('../globalFix');
    });

    expect(window.isNumber).toBe(original);
    expect(window.isNumber()).toBe('custom');
  });
});
