import mysql from 'mysql2/promise';
import { loadAppConfig } from './config/loadConfig.js';
import { TokenResolver } from './auth.js';
import { createApp } from './app.js';
import { MysqlIssueNotifyRepository } from './repositories/mysqlIssueNotifyRepository.js';
import { DefaultIssueNotifyService } from './services/issueNotifyService.js';
import { NacosRegistry } from './clients/nacosRegistry.js';
import { IssueNotifySocket } from './socket/issueNotifySocket.js';

const config = loadAppConfig();

const mysqlPool = mysql.createPool({
  host: config.mysql.host,
  port: config.mysql.port,
  user: config.mysql.user,
  password: config.mysql.password,
  database: config.mysql.database,
  connectionLimit: config.mysql.poolSize,
  supportBigNumbers: true,
  bigNumberStrings: true
});

const tokenResolver = new TokenResolver(config.jwt.secret);
const repository = new MysqlIssueNotifyRepository(mysqlPool);
const issueNotifyService = new DefaultIssueNotifyService(repository, config);

const app = createApp({
  config,
  tokenResolver,
  services: {
    issueNotifyService
  }
});

const nacosRegistry = new NacosRegistry(config.nacos);

await app.listen({
  host: config.server.host,
  port: config.server.port
});

const socketServer = new IssueNotifySocket(app.server, {
  config,
  tokenResolver,
  issueNotifyService
});

await nacosRegistry.register();
nacosRegistry.startHeartbeat();

async function shutdown() {
  socketServer.close();
  nacosRegistry.stopHeartbeat();
  await nacosRegistry.deregister().catch(() => undefined);
  await app.close().catch(() => undefined);
  await mysqlPool.end().catch(() => undefined);
}

process.on('SIGINT', () => {
  shutdown().finally(() => process.exit(0));
});

process.on('SIGTERM', () => {
  shutdown().finally(() => process.exit(0));
});
