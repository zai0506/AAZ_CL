// 預設匯率配置（以 TWD 為基準）
const TWD_RATES = {
  'JPY-TWD': 0.21,
  'USD-TWD': 31.5,
  'EUR-TWD': 34.2,
  'CNY-TWD': 4.35,
  'GBP-TWD': 39.8,
  'KRW-TWD': 0.024,
  'THB-TWD': 0.90,
  'SGD-TWD': 23.4,
  'HKD-TWD': 4.05,
  'AUD-TWD': 20.5,
};

// 建立完整的雙向匯率表
const DEFAULT_RATES = {};

// 加入 TWD 對其他貨幣的匯率
Object.entries(TWD_RATES).forEach(([key, value]) => {
  DEFAULT_RATES[key] = value;
});

// 計算反向匯率（其他貨幣對 TWD）
Object.entries(TWD_RATES).forEach(([key, value]) => {
  const [from, to] = key.split('-');
  const reverseKey = `${to}-${from}`;
  DEFAULT_RATES[reverseKey] = 1 / value;
});

/**
 * 取得預設匯率
 * @param {string} from - 來源貨幣代碼
 * @param {string} to - 目標貨幣代碼
 * @returns {number|null} 匯率數字，如果找不到則回傳 null
 */
export function getDefaultRate(from, to) {
  // 同一貨幣
  if (from === to) {
    return 1.0;
  }

  // 查詢預設匯率表
  const key = `${from}-${to}`;
  return DEFAULT_RATES[key] || null;
}

/**
 * 支援的貨幣列表
 */
export const SUPPORTED_CURRENCIES = [
  'TWD',
  'JPY',
  'USD',
  'EUR',
  'CNY',
  'GBP',
  'KRW',
  'THB',
  'SGD',
  'HKD',
  'AUD',
];
