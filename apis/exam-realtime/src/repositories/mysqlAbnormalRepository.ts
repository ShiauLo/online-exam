import type { Pool } from 'mysql2/promise';

export interface SaveAbnormalRecord {
  abnormalId: string;
  examId: number;
  reporterId: number;
  reporterRoleId: number;
  type: string;
  description: string;
  imgUrlsJson: string;
  screenOutCount: number | null;
  requestId: string;
  createTime: Date;
}

export class MysqlAbnormalRepository {
  constructor(private readonly pool: Pool) {}

  async save(record: SaveAbnormalRecord) {
    await this.pool.execute(
      `INSERT INTO exam_realtime_abnormal_record
       (abnormal_id, exam_id, reporter_id, reporter_role_id, type, description, img_urls,
        screen_out_count, request_id, create_time, update_time)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        record.abnormalId,
        record.examId,
        record.reporterId,
        record.reporterRoleId,
        record.type,
        record.description,
        record.imgUrlsJson,
        record.screenOutCount,
        record.requestId,
        record.createTime,
        record.createTime
      ]
    );
  }
}
