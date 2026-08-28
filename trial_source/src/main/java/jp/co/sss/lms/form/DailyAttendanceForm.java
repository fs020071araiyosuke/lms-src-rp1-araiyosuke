package jp.co.sss.lms.form;

import lombok.Data;

/**
 * 日次の勤怠フォーム
 * 
 * @author 東京ITスクール
 */
@Data
public class DailyAttendanceForm {

	/** 受講生勤怠ID */
	private Integer studentAttendanceId;
	/** 途中退校日 */
	private String leaveDate;
	/** 日付 */
	private String trainingDate;
	
//    /** Task26/Task27：出勤（時・分） */
//    private String trainingStartHour;
//    private String trainingStartMinute;
//
//    /** Task26/Task27：退勤（時・分） */
//    private String trainingEndHour;
//    private String trainingEndMinute;
//	
//	/** 出勤時間 */
//	private String trainingStartTime;
//	/** 退勤時間 */
//	private String trainingEndTime;
	
	/** 出勤時間（文字列：HH:mm） */
    private String trainingStartTime;

    /** 退勤時間（文字列：HH:mm） */
    private String trainingEndTime;

    /** 出勤時間（時） — Task26 */
    private Integer trainingStartTimeHour;

    /** 出勤時間（分） — Task26 */
    private Integer trainingStartTimeMinute;

    /** 退勤時間（時） — Task26 */
    private Integer trainingEndTimeHour;

    /** 退勤時間（分） — Task26 */
    private Integer trainingEndTimeMinute;
	
	
	/** 中抜け時間 */
	private Integer blankTime;
	/** 中抜け時間（画面表示用） */
	private String blankTimeValue;
	/** ステータス */
	private String status;
	/** 備考 */
	private String note;
	/** セクション名 */
	private String sectionName;
	/** 当日フラグ */
	private Boolean isToday;
	/** エラーフラグ */
	private Boolean isError;
	/** 日付（画面表示用） */
	private String dispTrainingDate;
	/** ステータス（画面表示用） */
	private String statusDispName;
	/** LMSユーザーID */
	private String lmsUserId;
	/** ユーザー名 */
	private String userName;
	/** コース名 */
	private String courseName;
	/** インデックス */
	private String index;

}
