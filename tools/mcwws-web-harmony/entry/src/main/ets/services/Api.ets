import { http } from '@kit.NetworkKit';

export const ORIGIN: string = 'https://mcs.ryanstudio.work';
export const MAP_ORIGIN: string = 'https://mcsmap.ryanstudio.work';
export const SESSION: string = 'mcwws-session';
export const CART: string = 'mcwws-native-cart';

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

interface ErrorResponse {
  error?: string;
}

export async function request<T>(path: string, token: string = '', body?: object): Promise<T> {
  const client = http.createHttp();
  const headers: Record<string, string> = { 'Accept': 'application/json' };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  try {
    const response = await client.request(ORIGIN + path, {
      method: body === undefined ? http.RequestMethod.GET : http.RequestMethod.POST,
      header: headers,
      extraData: body === undefined ? undefined : JSON.stringify(body),
      expectDataType: http.HttpDataType.STRING,
      connectTimeout: 8000,
      readTimeout: 15000
    });
    const raw = response.result as string;
    if (response.responseCode < 200 || response.responseCode >= 300) {
      let message = '请求失败（' + response.responseCode + '）';
      try {
        message = (JSON.parse(raw) as ErrorResponse).error || message;
      } catch {
        // Gateways can return a non-JSON error page.
      }
      throw new ApiError(response.responseCode, message);
    }
    return JSON.parse(raw) as T;
  } finally {
    client.destroy();
  }
}

export function errorMessage(error: Error): string {
  return error instanceof ApiError ? error.message : '连接失败，请检查网络后重试。';
}

export function money(amount: number | null | undefined): string {
  return typeof amount === 'number' && Number.isFinite(amount) ? '￥' + amount.toFixed(2) : '—';
}

export function dateText(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN');
}

export class ListSource<T> implements IDataSource {
  private values: T[] = [];
  private listeners: DataChangeListener[] = [];
  totalCount(): number {
    return this.values.length;
  }
  getData(index: number): T {
    return this.values[index];
  }
  registerDataChangeListener(listener: DataChangeListener): void {
    this.listeners.push(listener);
  }
  unregisterDataChangeListener(listener: DataChangeListener): void {
    this.listeners = this.listeners.filter(item => item !== listener);
  }
  set(values: T[]): void {
    this.values = values;
    this.listeners.forEach(listener => listener.onDataReloaded());
  }
}
