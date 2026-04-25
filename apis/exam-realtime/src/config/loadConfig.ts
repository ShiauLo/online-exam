import fs from 'node:fs';
import path from 'node:path';
import YAML from 'yaml';
import { appConfigSchema } from './env.js';
import type { AppConfig } from '../types.js';

type LooseObject = Record<string, unknown>;

function readIfExists(filePath: string) {
  if (!fs.existsSync(filePath)) {
    return undefined;
  }
  return fs.readFileSync(filePath, 'utf-8');
}

function resolvePlaceholders(value: string, env: NodeJS.ProcessEnv) {
  return value.replace(/\$\{([^}]+)\}/g, (_, name: string) => env[name] ?? '');
}

function deepMerge(target: LooseObject, source: LooseObject) {
  const next: LooseObject = { ...target };
  for (const [key, value] of Object.entries(source)) {
    if (
      value &&
      typeof value === 'object' &&
      !Array.isArray(value) &&
      next[key] &&
      typeof next[key] === 'object' &&
      !Array.isArray(next[key])
    ) {
      next[key] = deepMerge(next[key] as LooseObject, value as LooseObject);
      continue;
    }
    next[key] = value;
  }
  return next;
}

function assignByPath(target: LooseObject, dottedKey: string, value: string) {
  const keys = dottedKey.split('.');
  let current = target;
  keys.forEach((key, index) => {
    if (index === keys.length - 1) {
      current[key] = value;
      return;
    }
    if (!current[key] || typeof current[key] !== 'object' || Array.isArray(current[key])) {
      current[key] = {};
    }
    current = current[key] as LooseObject;
  });
}

function parseProperties(content: string, env: NodeJS.ProcessEnv) {
  const result: LooseObject = {};
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }
    const separatorIndex = trimmed.indexOf('=');
    if (separatorIndex < 0) {
      continue;
    }
    const key = trimmed.slice(0, separatorIndex).trim();
    const value = resolvePlaceholders(trimmed.slice(separatorIndex + 1).trim(), env);
    assignByPath(result, key, value);
  }
  return result;
}

function parseYaml(content: string, env: NodeJS.ProcessEnv) {
  return YAML.parse(resolvePlaceholders(content, env)) as LooseObject;
}

function applyEnvOverrides(base: LooseObject, env: NodeJS.ProcessEnv) {
  const overrides: LooseObject = {};
  const mappings: Array<[string, string]> = [
    ['mysql.host', 'EXAM_DB_HOST'],
    ['mysql.port', 'EXAM_DB_PORT'],
    ['mysql.database', 'EXAM_DB_NAME'],
    ['mysql.user', 'EXAM_DB_USERNAME'],
    ['mysql.password', 'EXAM_DB_PASSWORD'],
    ['redis.host', 'EXAM_REDIS_HOST'],
    ['redis.port', 'EXAM_REDIS_PORT'],
    ['redis.database', 'EXAM_REDIS_DATABASE'],
    ['redis.password', 'EXAM_REDIS_PASSWORD'],
    ['nacos.enabled', 'EXAM_NACOS_ENABLED'],
    ['nacos.serverAddr', 'EXAM_NACOS_ADDR'],
    ['nacos.username', 'EXAM_NACOS_USERNAME'],
    ['nacos.password', 'EXAM_NACOS_PASSWORD'],
    ['nacos.serviceIp', 'EXAM_REALTIME_IP'],
    ['nacos.servicePort', 'EXAM_REALTIME_PORT'],
    ['downstream.examCoreBaseUrl', 'EXAM_REALTIME_EXAM_CORE_BASE_URL'],
    ['jwt.secret', 'EXAM_JWT_SECRET'],
    ['server.port', 'PORT'],
    ['server.host', 'HOST'],
    ['server.issueSocketPath', 'EXAM_ISSUE_SOCKET_PATH']
  ];

  for (const [configKey, envKey] of mappings) {
    const value = env[envKey];
    if (value !== undefined && value !== '') {
      assignByPath(overrides, configKey, value);
    }
  }

  return deepMerge(base, overrides);
}

export function loadAppConfig(
  baseDir = process.cwd(),
  env: NodeJS.ProcessEnv = process.env
): AppConfig {
  const configDir = path.resolve(baseDir, 'config');
  const application = readIfExists(path.join(configDir, 'application.yaml'));
  const applicationDev = readIfExists(path.join(configDir, 'application-dev.yaml'));
  const applicationLocal = readIfExists(path.join(configDir, 'application-local.properties'));

  let merged: LooseObject = {};
  if (application) {
    merged = deepMerge(merged, parseYaml(application, env));
  }
  if (applicationDev) {
    merged = deepMerge(merged, parseYaml(applicationDev, env));
  }
  if (applicationLocal) {
    merged = deepMerge(merged, parseProperties(applicationLocal, env));
  }
  merged = applyEnvOverrides(merged, env);

  return appConfigSchema.parse(merged) as AppConfig;
}
