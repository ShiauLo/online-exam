import { z } from 'zod';

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
    port: toPositiveInt(8091),
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
  nacos: z.object({
    enabled: toBoolean(true),
    serverAddr: z.string().min(1),
    username: z.string().min(1),
    password: z.string(),
    namespace: z.string().min(1),
    group: z.string().min(1),
    serviceIp: z.string().min(1),
    servicePort: toPositiveInt(8091),
    heartbeatIntervalMs: toPositiveInt(5000)
  }),
  jwt: z.object({
    secret: z.string().min(32)
  }),
  notify: z.object({
    defaultLimit: toPositiveInt(20),
    maxLimit: toPositiveInt(100),
    pollIntervalMs: toPositiveInt(3000),
    historyLookbackMinutes: toPositiveInt(1440)
  })
});
