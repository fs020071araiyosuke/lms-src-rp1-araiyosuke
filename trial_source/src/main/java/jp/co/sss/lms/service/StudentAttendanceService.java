package jp.co.sss.lms.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	/**
	 * 過去日未入力チェック
	 * @author 新井陽介Task.25
	 * @return true:過去日に未入力がある
	 * 			false:未入力はない
	 * @throws ParseException 
	 */
	public boolean notEnterCheck() throws ParseException {
		// "yyyy-MM-dd"形式で日付のみを扱うフォーマットを準備
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// 現在日時（時刻あり）を取得
		Date now = new Date();
		// 現在日時を"yyyy-MM-dd"の文字列に変換
		String formattedToday = sdf.format(now);

		Date trainingDate = sdf.parse(formattedToday);

		Integer count = tStudentAttendanceMapper.notEnterCount(
				loginUserDto.getLmsUserId(),
				trainingDate,
				Constants.DB_FLG_FALSE);

		// 未入力件数が1件以上ならtrueを返す
		return count > 0;
	}

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		default:
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());

		// task26:時・分の選択用マップ生成（00〜23 / 00〜59）
		attendanceForm.setStartHourMap(createHourMap());
		attendanceForm.setEndHourMap(createHourMap());
		attendanceForm.setStartMinuteMap(createMinuteMap());
		attendanceForm.setEndMinuteMap(createMinuteMap());

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm.setLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// DTO → DailyAttendanceForm へコピー＋時間分割
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {

			DailyAttendanceForm daily = new DailyAttendanceForm();
			daily.setStudentAttendanceId(dto.getStudentAttendanceId());

			// ★★★ 最重要修正ポイント ★★★
			// trainingDate を yyyy-MM-dd に統一する（update() が parse できる形式）
			daily.setTrainingDate(
					dateUtil.dateToString(dto.getTrainingDate(), "yyyy-MM-dd"));

			daily.setTrainingStartTime(dto.getTrainingStartTime());
			daily.setTrainingEndTime(dto.getTrainingEndTime());

			// 出勤時間 hh:mm → [時][分] に分割
			if (dto.getTrainingStartTime() != null && dto.getTrainingStartTime().length() == 5) {
				String timeString = dto.getTrainingStartTime();
				daily.setTrainingStartHour(timeString.substring(0, 2));
				daily.setTrainingStartMinute(timeString.substring(3, 5));
			}

			// 退勤時間 hh:mm → [時][分] に分割
			if (dto.getTrainingEndTime() != null && dto.getTrainingEndTime().length() == 5) {
				String timeString = dto.getTrainingEndTime();
				daily.setTrainingEndHour(timeString.substring(0, 2));
				daily.setTrainingEndMinute(timeString.substring(3, 5));
			}

			// 中抜け
			if (dto.getBlankTime() != null) {
				daily.setBlankTime(dto.getBlankTime());
				daily.setBlankTimeValue(
						String.valueOf(attendanceUtil.calcBlankTime(dto.getBlankTime())));
			}

			daily.setStatus(String.valueOf(dto.getStatus()));
			daily.setNote(dto.getNote());
			daily.setSectionName(dto.getSectionName());
			daily.setIsToday(dto.getIsToday());

			// 表示用日付（こちらは yyyy年M月d日(E) のままでOK）
			daily.setDispTrainingDate(
					dateUtil.dateToString(dto.getTrainingDate(), "yyyy年M月d日(E)"));

			daily.setStatusDispName(dto.getStatusDispName());

			attendanceForm.getAttendanceList().add(daily);
		}

		return attendanceForm;
	}

	/**task26
	 * 時間マップ生成（00〜23）
	 */
	public LinkedHashMap<Integer, String> createHourMap() {
		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
		for (int h = 0; h <= 23; h++) {
			String value = String.format("%02d", h);
			map.put(h, value);
		}
		return map;
	}

	/**task26
	 * 分マップ生成（00～59）
	 */
	public LinkedHashMap<Integer, String> createMinuteMap() {
		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
		for (int m = 0; m <= 59; m++) {
			String value = String.format("%02d", m);
			map.put(m, value);
		}
		return map;
	}

	/**
	* 中抜け時間マップ取得（画面再表示用）
	*/
	public LinkedHashMap<Integer, String> getBlankTimeMap() {
		return attendanceUtil.setBlankTime();
	}

	/**
	 * メソッド名：formatConversion  
	 * 入力された出退勤の時間値を hh:mm 形式に変換し、AttendanceForm にセット
	 */
	public void formatConversion(AttendanceForm attendanceForm) {

		if (attendanceForm.getAttendanceList() == null) {
			return;
		}

		for (DailyAttendanceForm daily : attendanceForm.getAttendanceList()) {

			// 始業時間 [時][分] → hh:mm
			if (daily.getTrainingStartHour() != null
					&& !daily.getTrainingStartHour().isEmpty()
					&& daily.getTrainingStartMinute() != null
					&& !daily.getTrainingStartMinute().isEmpty()) {

				try {
					int h = Integer.parseInt(daily.getTrainingStartHour());
					int m = Integer.parseInt(daily.getTrainingStartMinute());
					daily.setTrainingStartTime(String.format("%02d:%02d", h, m));
				} catch (NumberFormatException e) {
					daily.setTrainingStartTime(null);
				}
			} else {
				daily.setTrainingStartTime(null);
			}

			// 終業時間 [時][分] → hh:mm
			if (daily.getTrainingEndHour() != null
					&& !daily.getTrainingEndHour().isEmpty()
					&& daily.getTrainingEndMinute() != null
					&& !daily.getTrainingEndMinute().isEmpty()) {

				try {
					int h = Integer.parseInt(daily.getTrainingEndHour());
					int m = Integer.parseInt(daily.getTrainingEndMinute());
					daily.setTrainingEndTime(String.format("%02d:%02d", h, m));
				} catch (NumberFormatException e) {
					daily.setTrainingEndTime(null);
				}
			} else {
				daily.setTrainingEndTime(null);
			}
		}
	}

	/**
	 * 空欄を null に変換する
	 */
	private String toNullIfEmpty(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return value;
	}

	/**
	* Task.27 勤怠入力チェック
	*/
	public void updateInputCheck(AttendanceForm attendanceForm,
			BindingResult result) {

		if (attendanceForm.getAttendanceList() == null) {
			return;
		}

		int index = 0;
		for (DailyAttendanceForm daily : attendanceForm.getAttendanceList()) {

			String fieldPrefix = "attendanceList[" + index + "]";

			// 備考文字数チェック（100文字）
			if (daily.getNote() != null && daily.getNote().length() > 100) {
				result.addError(new FieldError(
						result.getObjectName(),
						fieldPrefix + ".note",
						messageUtil.getMessage("maxlength",
								new String[] { "備考", "100" })));
			}

			// 出勤時間の片側入力チェック
			boolean startHourEmpty = (daily.getTrainingStartHour() == null
					|| daily.getTrainingStartHour().isEmpty());
			boolean startMinuteEmpty = (daily.getTrainingStartMinute() == null
					|| daily.getTrainingStartMinute().isEmpty());

			if ((startHourEmpty && !startMinuteEmpty)
					|| (!startHourEmpty && startMinuteEmpty)) {
				result.addError(new FieldError(
						result.getObjectName(),
						fieldPrefix + ".trainingStartHour",
						messageUtil.getMessage("input.invalid",
								new String[] { "出勤時間" })));
			}

			// 退勤時間の片側入力チェック
			boolean endHourEmpty = (daily.getTrainingEndHour() == null
					|| daily.getTrainingEndHour().isEmpty());
			boolean endMinuteEmpty = (daily.getTrainingEndMinute() == null
					|| daily.getTrainingEndMinute().isEmpty());

			if ((endHourEmpty && !endMinuteEmpty)
					|| (!endHourEmpty && endMinuteEmpty)) {
				result.addError(new FieldError(
						result.getObjectName(),
						fieldPrefix + ".trainingEndHour",
						messageUtil.getMessage("input.invalid",
								new String[] { "退勤時間" })));
			}

			// 出勤なし＆退勤ありチェック
			if (startHourEmpty && startMinuteEmpty
					&& (!endHourEmpty || !endMinuteEmpty)) {
				result.addError(new FieldError(
						result.getObjectName(),
						fieldPrefix + ".trainingStartHour",
						messageUtil.getMessage(
								"attendance.punchInEmpty", null)));
			}

			// hh:mm に変換してから出勤＞退勤チェック
			formatConversion(attendanceForm);

			if (daily.getTrainingStartTime() != null
					&& daily.getTrainingEndTime() != null) {

				TrainingTime start = new TrainingTime(daily.getTrainingStartTime());
				TrainingTime end = new TrainingTime(daily.getTrainingEndTime());

				if (start.compareTo(end) > 0) {
					result.addError(new FieldError(
							result.getObjectName(),
							fieldPrefix + ".trainingEndHour",
							messageUtil.getMessage(
									"attendance.trainingTimeRange",
									new String[] { String.valueOf(index + 1) })));
				}

				// 中抜け時間が勤務時間を超えるチェック（簡易）
				if (daily.getBlankTime() != null) {
					TrainingTime blank = attendanceUtil.calcBlankTime(daily.getBlankTime());
					if (blank.compareTo(end) > 0) {
						result.addError(new FieldError(
								result.getObjectName(),
								fieldPrefix + ".blankTime",
								messageUtil.getMessage(
										"attendance.blankTimeError", null)));
					}
				}
			}

			index++;
		}
	}

	/**
	 * Task27：勤怠入力チェック
	 */
	//	public void updateInputCheck(AttendanceForm attendanceForm, BindingResult result)
	//			throws ParseException {
	//
	//		for (int i = 0; i < attendanceForm.getAttendanceList().size(); i++) {
	//
	//			DailyAttendanceForm daily = attendanceForm.getAttendanceList().get(i);
	//			String indexName = "attendanceList[" + i + "]";
	//
	//			// 備考100文字チェック
	//			if (daily.getNote() != null && daily.getNote().length() > 100) {
	//				result.addError(new FieldError(result.getObjectName(),
	//						indexName + ".note",
	//						messageUtil.getMessage("maxlength", new String[] { "備考", "100" })));
	//			}
	//
	//			// 出勤・退勤の片側入力チェック
	//			boolean startHour = daily.getTrainingStartHour() != null && !daily.getTrainingStartHour().isEmpty();
	//			boolean startMin = daily.getTrainingStartMinute() != null && !daily.getTrainingStartMinute().isEmpty();
	//			boolean endHour = daily.getTrainingEndHour() != null && !daily.getTrainingEndHour().isEmpty();
	//			boolean endMin = daily.getTrainingEndMinute() != null && !daily.getTrainingEndMinute().isEmpty();
	//
	//			if (startHour ^ startMin) {
	//				result.addError(new FieldError(result.getObjectName(),
	//						indexName + ".trainingStartHour",
	//						messageUtil.getMessage("input.invalid", new String[] { "出勤時間" })));
	//			}
	//
	//			if (endHour ^ endMin) {
	//				result.addError(new FieldError(result.getObjectName(),
	//						indexName + ".trainingEndHour",
	//						messageUtil.getMessage("input.invalid", new String[] { "退勤時間" })));
	//			}
	//
	//			// 出勤なし退勤ありチェック
	//			if (!startHour && !startMin && (endHour || endMin)) {
	//				result.addError(new FieldError(result.getObjectName(),
	//						indexName + ".trainingEndHour",
	//						messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY)));
	//			}
	//
	//			// 出勤＞退勤チェック
	//			if (startHour && startMin && endHour && endMin) {
	//				TrainingTime start = new TrainingTime(
	//						String.format("%02d:%02d",
	//								Integer.parseInt(daily.getTrainingStartHour()),
	//								Integer.parseInt(daily.getTrainingStartMinute())));
	//
	//				TrainingTime end = new TrainingTime(
	//						String.format("%02d:%02d",
	//								Integer.parseInt(daily.getTrainingEndHour()),
	//								Integer.parseInt(daily.getTrainingEndMinute())));
	//
	//				if (start.compareTo(end) > 0) {
	//					result.addError(new FieldError(result.getObjectName(),
	//							indexName + ".trainingEndHour",
	//							messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE,
	//									new String[] { String.valueOf(i + 1) })));
	//				}
	//
	//				// 中抜け時間チェック
	//				if (daily.getBlankTime() != null) {
	//					TrainingTime diff = attendanceUtil.calcJukoTime(start, end);
	//					TrainingTime blank = attendanceUtil.calcBlankTime(daily.getBlankTime());
	//
	//					if (blank.compareTo(diff) > 0) {
	//						result.addError(new FieldError(result.getObjectName(),
	//								indexName + ".blankTime",
	//								messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_BLANKTIMEERROR)));
	//					}
	//				}
	//			}
	//		}
	//	}

	/**
	 * 勤怠登録・更新処理（空欄は未入力として扱う完全版）
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		// 時刻を hh:mm に組み立てる（空欄は null）
		formatConversion(attendanceForm);

		Integer lmsUserId = loginUserUtil.isStudent()
				? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 既存勤怠情報を取得
		List<TStudentAttendance> oldList = tStudentAttendanceMapper.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		Date now = new Date();
		List<TStudentAttendance> saveList = new ArrayList<>();

		for (DailyAttendanceForm daily : attendanceForm.getAttendanceList()) {

			// 研修日を Date に変換
			Date trainingDate = dateUtil.parse(daily.getTrainingDate());

			// 既存レコード判定
			TStudentAttendance entity = oldList.stream()
					.filter(o -> o.getTrainingDate().equals(trainingDate))
					.findFirst()
					.orElse(null);

			if (entity == null) {
				// 新規作成
				entity = new TStudentAttendance();
				entity.setTrainingDate(trainingDate);
				entity.setFirstCreateUser(loginUserDto.getLmsUserId());
				entity.setFirstCreateDate(now);
			}

			// 共通項目
			entity.setLmsUserId(lmsUserId);
			entity.setAccountId(loginUserDto.getAccountId());
			entity.setLastModifiedUser(loginUserDto.getLmsUserId());
			entity.setLastModifiedDate(now);
			entity.setDeleteFlg(Constants.DB_FLG_FALSE);

			//try-catch
			//			if (daily.getTrainingStartHour() != null && !daily.getTrainingStartHour().isEmpty()
			//					&& daily.getTrainingStartMinute() != null && !daily.getTrainingStartMinute().isEmpty()) {
			//
			//				try {
			//					int h = Integer.parseInt(daily.getTrainingStartHour());
			//					int m = Integer.parseInt(daily.getTrainingStartMinute());
			//					daily.setTrainingStartTime(String.format("%02d:%02d", h, m));
			//				} catch (NumberFormatException e) {
			//					daily.setTrainingStartTime(null);
			//				}
			//			} else {
			//				daily.setTrainingStartTime(null);
			//			}

			// ★ 出勤時刻（空欄は null）
			String start = toNullIfEmpty(daily.getTrainingStartTime());
			if (start != null) {
				entity.setTrainingStartTime(new TrainingTime(start).getFormattedString());
			} else {
				entity.setTrainingStartTime(null);
			}

			// ★ 退勤時刻（空欄は null）
			String end = toNullIfEmpty(daily.getTrainingEndTime());
			if (end != null) {
				entity.setTrainingEndTime(new TrainingTime(end).getFormattedString());
			} else {
				entity.setTrainingEndTime(null);
			}

			// ★ 中抜け（空欄は null）
			entity.setBlankTime(daily.getBlankTime() != null ? daily.getBlankTime() : null);

			// ★ 欠席なら時刻を消す
			if ("欠席".equals(daily.getStatusDispName())) {
				entity.setStatus(AttendanceStatusEnum.ABSENT.code);
				entity.setTrainingStartTime(null);
				entity.setTrainingEndTime(null);
			} else {
				// 遅刻・早退判定
				TrainingTime startTime = entity.getTrainingStartTime() != null
						? new TrainingTime(entity.getTrainingStartTime())
						: null;

				TrainingTime endTime = entity.getTrainingEndTime() != null
						? new TrainingTime(entity.getTrainingEndTime())
						: null;

				AttendanceStatusEnum statusEnum = attendanceUtil.getStatus(startTime, endTime);
				if (statusEnum != null) {
					entity.setStatus(statusEnum.code);
				}
			}

			// 備考
			entity.setNote(daily.getNote());

			saveList.add(entity);
		}

		// 登録・更新処理
		for (TStudentAttendance entity : saveList) {
			if (entity.getStudentAttendanceId() == null) {
				tStudentAttendanceMapper.insert(entity);
			} else {
				tStudentAttendanceMapper.update(entity);
			}
		}

		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}
}
