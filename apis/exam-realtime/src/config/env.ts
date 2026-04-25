import { z } from 'zod';

const numericValue = (defaultValue: number) =>
  z.preprocess((value) => {
    if (value === undefined || value === null || value === '') {
      return defaultValue;
    }
    if (typeof value === 'number') {
      return value;
    }
    return Number(value);
  }, z.number().finite());

const toPositiveInt = (defaultValue: number) =>
  z.preprocess((value) => {
    if (value === undefined || value === null || value === '') {
      return defaultValue;
    }
    if (typeof value === 'number') {
      return value;
    }
    return Number(value);
  }, z.number().int().positive());

const toNonNegativeInt = (defaultValue: number) =>
  z.preprocess((value) => {
    if (value === undefined || value === null || value === '') {
      return defaultValue;
    }
    if (typeof value === 'number') {
      return value;
    }
    return Number(value);
  }, z.number().int().min(0));

const toBoolean = (defaultValue: boolean) =>
  z.preprocess((value) => {
    if (value === undefined || value === null || value === '') {
      return defaultValue;
    }
    if (typeof value === 'boolean') {
      return value;
    }
    return String(value).toLowerCase() === 'true';
  }, z.boolean());

export const appConfigSchema = z.object({
  app: z.object({
    name: z.string().min(1),
    profiles: z.string().min(1)
  }),
  server: z.object({
    host: z.string().min(1),
    port: toPositiveInt(8090),
    socketPath: z.string().min(1),
    corsOrigins: z.string().min(1)
  }),
  mysql: z.object({
    host: z.string().min(1),
    port: toPositiveInt(3306),
    database: z.string().min(1),
    user: z.string().min(1),
    password: z.string(),
    poolSize: toPositiveInt(10)
  }),
  redis: z.object({
    host: z.string().min(1),
    port: toPositiveInt(6379),
    database: toNonNegativeInt(0),
    password: z.string()
  }),
  nacos: z.object({
    enabled: toBoolean(true),
    serverAddr: z.string().min(1),
    username: z.string().min(1),
    password: z.string(),
    namespace: z.string().min(1),
    group: z.string().min(1),
    serviceIp: z.string().min(1),
    servicePort: toPositiveInt(8090),
    heartbeatIntervalMs: toPositiveInt(5000)
  }),
  downstream: z.object({
    examCoreBaseUrl: z.string().url()
  }),
  jwt: z.object({
    secret: z.string().min(32)
  }),
  realtime: z.object({
    ttlHoursAfterExam: toPositiveInt(24),
    autoSubmitLockSeconds: toPositiveInt(120),
    countdownIntervalMs: toPositiveInt(1000)
  })
});
