import mysql from 'mysql2/promise';
import { createClient } from 'redis';
import { loadAppConfig } from './config/loadConfig.js';
import { TokenResolver } from './auth.js';
import { createApp } from './app.js';
import { MysqlExamReadRepository } from './repositories/mysqlExamReadRepository.js';
import { RedisRealtimeRepository } from './repositories/redisRealtimeRepository.js';
import { MysqlAbnormalRepository } from './repositories/mysqlAbnormalRepository.js';
import { DefaultSessionService } from './services/sessionService.js';
import { DefaultProgressService } from './services/progressService.js';
import { DefaultSubmitService } from './services/submitService.js';
import { DefaultAbnormalService } from './services/abnormalService.js';
import { DefaultIssueNotifyService } from './services/issueNotifyService.js';
import { ExamCoreClient } from './clients/examCoreClient.js';
import { NacosRegistry } from './clients/nacosRegistry.js';
import { ExamRealtimeSocket } from './socket/examRealtimeSocket.js';
import { IssueNotifySocket } from './socket/issueNotifySocket.js';
import { MysqlIssueNotifyRepository } from './repositories/mysqlIssueNotifyRepository.js';

const config = loadAppConfig();

const mysqlPool = mysql.createPool({
  host: config.mysql.host,
  port: config.mysql.port,
  user: config.mysql.user,
  password: config.mysql.password,
  database: config.mysql.database,
  connectionLimit: config.mysql.poolSize
});

const redisClient = createClient({
  socket: {
    host: config.redis.host,
    port: config.redis.port
  },
  password: config.redis.password || undefined,
  database: config.redis.database
});

await redisClient.connect();

const examRepository = new MysqlExamReadRepository(mysqlPool);
const redisRepository = new RedisRealtimeRepository(redisClient);
const abnormalRepository = new MysqlAbnormalRepository(mysqlPool);
const issueNotifyRepository = new MysqlIssueNotifyRepository(mysqlPool);
const examCoreClient = new ExamCoreClient(config.downstream.examCoreBaseUrl);
const tokenResolver = new TokenResolver(config.jwt.secret);
const sessionService = new DefaultSessionService(examRepository, redisRepository, config);
const progressService = new DefaultProgressService(examRepository, redisRepository, config);
const submitService = new DefaultSubmitService(
  examRepository,
  redisRepository,
  examCoreClient,
  config
);
const abnormalService = new DefaultAbnormalService(
  abnormalRepository,
  examRepository,
  redisRepository,
  config
);
const issueNotifyService = new DefaultIssueNotifyService(issueNotifyRepository, config);

const app = createApp({
  config,
  tokenResolver,
  services: {
    sessionService,
    progressService,
    submitService,
    abnormalService,
    issueNotifyService
  }
});

const nacosRegistry = new NacosRegistry(config.nacos);

await app.listen({
  host: config.server.host,
  port: config.server.port
});

const socketServer = new ExamRealtimeSocket(app.server, {
  config,
  tokenResolver,
  sessionService,
  progressService,
  submitService,
  redisRepository
});
const issueNotifySocketServer = new IssueNotifySocket(app.server, {
  config,
  tokenResolver,
  issueNotifyService
});

await nacosRegistry.register();
nacosRegistry.startHeartbeat();

async function shutdown() {
  socketServer.close();
  issueNotifySocketServer.close();
  nacosRegistry.stopHeartbeat();
  await nacosRegistry.deregister().catch(() => undefined);
  await app.close().catch(() => undefined);
  await redisClient.quit().catch(() => undefined);
  await mysqlPool.end().catch(() => undefined);
}

process.on('SIGINT', () => {
  shutdown().finally(() => process.exit(0));
});

process.on('SIGTERM', () => {
  shutdown().finally(() => process.exit(0));
});
