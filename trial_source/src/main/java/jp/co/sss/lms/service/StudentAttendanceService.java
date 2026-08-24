//package jp.co.sss.lms.service;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.LinkedHashMap;
//import java.util.List;
//
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import jp.co.sss.lms.dto.AttendanceManagementDto;
//import jp.co.sss.lms.dto.LoginUserDto;
//import jp.co.sss.lms.entity.TStudentAttendance;
//import jp.co.sss.lms.enums.AttendanceStatusEnum;
//import jp.co.sss.lms.form.AttendanceForm;
//import jp.co.sss.lms.form.DailyAttendanceForm;
//import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
//import jp.co.sss.lms.util.AttendanceUtil;
//import jp.co.sss.lms.util.Constants;
//import jp.co.sss.lms.util.DateUtil;
//import jp.co.sss.lms.util.LoginUserUtil;
//import jp.co.sss.lms.util.MessageUtil;
//import jp.co.sss.lms.util.TrainingTime;
//
//@Service
//public class StudentAttendanceService {
//
//	@Autowired
//	private DateUtil dateUtil;
//	@Autowired
//	private AttendanceUtil attendanceUtil;
//	@Autowired
//	private MessageUtil messageUtil;
//	@Autowired
//	private LoginUserUtil loginUserUtil;
//	@Autowired
//	private LoginUserDto loginUserDto;
//	@Autowired
//	private TStudentAttendanceMapper tStudentAttendanceMapper;
//
//	/**
//	 * 過去日未入力チェック
//	 * @author 新井陽介Task.25
//	 * @return true:過去日に未入力がある
//	 * 			false:未入力はない
//	 */
//	public boolean notEnterCheck() {
//		// "yyyy-MM-dd"形式で日付のみを扱うフォーマットを準備
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		// 現在日時（時刻あり）を取得
//		Date now = new Date();
//		// 現在日時を"yyyy-MM-dd"の文字列に変換
//		String formattedToday = sdf.format(now);
//
//		Date todayZeroTime;
//		try {
//			// 日付文字列をDateに変換
//			todayZeroTime = sdf.parse(formattedToday);
//		} catch (ParseException e) {
//			// SimpleDateFormat#parse が投げる可能性がある唯一の例外
//			// フォーマットが不正な場合に発生するが、format()で生成した文字列をparseしているため本来起こりえない
//			throw new RuntimeException("日付フォーマット変換に失敗しました", e);
//		}
//
//		// DBに対して「今日より前の日付で、未入力の勤怠があるか」を問い合わせる
//		// loginUserDto.getLmsUserId():ログイン中ユーザーのID
//		// todayZeroTime              : 今日の0時（これより前の日付を検索対象とする）
//		// Constants.DB_FLG_FALSE     : 未入力フラグ
//		Integer count = tStudentAttendanceMapper.notEnterCount(
//				loginUserDto.getLmsUserId(),
//				todayZeroTime,
//				Constants.DB_FLG_FALSE);
//
//		// 未入力件数が1件以上ならtrueを返す
//		return count > 0;
//	}
//
//	/**
//	 * 勤怠一覧取得
//	 */
//	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId, Integer lmsUserId) {
//
//		List<AttendanceManagementDto> list = tStudentAttendanceMapper.getAttendanceManagement(courseId, lmsUserId,
//				Constants.DB_FLG_FALSE);
//
//		for (AttendanceManagementDto dto : list) {
//
//			if (dto.getBlankTime() != null) {
//				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
//				dto.setBlankTimeValue(String.valueOf(blankTime));
//			}
//
//			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
//			if (statusEnum != null) {
//				dto.setStatusDispName(statusEnum.name);
//			}
//		}
//
//		return list;
//	}
//
//	/**
//	 * 勤怠フォーム設定
//	 */
//	public AttendanceForm setAttendanceForm(List<AttendanceManagementDto> dtoList) {
//
//		AttendanceForm form = new AttendanceForm();
//		form.setAttendanceList(new ArrayList<>());
//		form.setLmsUserId(loginUserDto.getLmsUserId());
//		form.setUserName(loginUserDto.getUserName());
//		form.setLeaveFlg(loginUserDto.getLeaveFlg());
//		form.setBlankTimes(attendanceUtil.setBlankTime());
//
//		form.setStartHourMap(createHourMap());
//		form.setEndHourMap(createHourMap());
//		form.setStartMinuteMap(createMinuteMap());
//		form.setEndMinuteMap(createMinuteMap());
//
//		if (loginUserDto.getLeaveDate() != null) {
//			form.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
//			form.setDispLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
//		}
//
//		for (AttendanceManagementDto dto : dtoList) {
//
//			DailyAttendanceForm daily = new DailyAttendanceForm();
//			daily.setStudentAttendanceId(dto.getStudentAttendanceId());
//			daily.setTrainingDate(dateUtil.toString(dto.getTrainingDate()));
//			daily.setTrainingStartTime(dto.getTrainingStartTime());
//			daily.setTrainingEndTime(dto.getTrainingEndTime());
//
//			if (dto.getTrainingStartTime() != null && dto.getTrainingStartTime().length() == 5) {
//				daily.setTrainingStartHour(dto.getTrainingStartTime().substring(0, 2));
//				daily.setTrainingStartMinute(dto.getTrainingStartTime().substring(3, 5));
//			}
//
//			if (dto.getTrainingEndTime() != null && dto.getTrainingEndTime().length() == 5) {
//				daily.setTrainingEndHour(dto.getTrainingEndTime().substring(0, 2));
//				daily.setTrainingEndMinute(dto.getTrainingEndTime().substring(3, 5));
//			}
//
//			if (dto.getBlankTime() != null) {
//				daily.setBlankTime(dto.getBlankTime());
//				daily.setBlankTimeValue(String.valueOf(attendanceUtil.calcBlankTime(dto.getBlankTime())));
//			}
//
//			daily.setStatus(String.valueOf(dto.getStatus()));
//			daily.setNote(dto.getNote());
//			daily.setSectionName(dto.getSectionName());
//			daily.setIsToday(dto.getIsToday());
//			daily.setDispTrainingDate(dateUtil.dateToString(dto.getTrainingDate(), "yyyy年M月d日(E)"));
//			daily.setStatusDispName(dto.getStatusDispName());
//
//			form.getAttendanceList().add(daily);
//		}
//
//		return form;
//	}
//
//	/** 時間マップ（00〜23） */
//	private LinkedHashMap<Integer, String> createHourMap() {
//		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
//		for (int h = 0; h <= 23; h++) {
//			map.put(h, String.format("%02d", h));
//		}
//		return map;
//	}
//
//	/** 分マップ（00〜59） */
//	private LinkedHashMap<Integer, String> createMinuteMap() {
//		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
//		for (int m = 0; m <= 59; m++) {
//			map.put(m, String.format("%02d", m));
//		}
//		return map;
//	}
//
//	/**
//	 * 時・分 → hh:mm
//	 */
//	public void formatConversion(AttendanceForm form) {
//
//		if (form == null || form.getAttendanceList() == null)
//			return;
//
//		for (DailyAttendanceForm daily : form.getAttendanceList()) {
//
//			if (daily.getTrainingStartHour() != null && daily.getTrainingStartMinute() != null
//					&& !daily.getTrainingStartHour().isEmpty() && !daily.getTrainingStartMinute().isEmpty()) {
//
//				daily.setTrainingStartTime(
//						String.format("%02d:%02d",
//								Integer.parseInt(daily.getTrainingStartHour()),
//								Integer.parseInt(daily.getTrainingStartMinute())));
//			} else {
//				daily.setTrainingStartTime(null);
//			}
//
//			if (daily.getTrainingEndHour() != null && daily.getTrainingEndMinute() != null
//					&& !daily.getTrainingEndHour().isEmpty() && !daily.getTrainingEndMinute().isEmpty()) {
//
//				daily.setTrainingEndTime(
//						String.format("%02d:%02d",
//								Integer.parseInt(daily.getTrainingEndHour()),
//								Integer.parseInt(daily.getTrainingEndMinute())));
//			} else {
//				daily.setTrainingEndTime(null);
//			}
//		}
//	}
//
//	/**
//	 * 勤怠登録・更新
//	 */
//	public String update(AttendanceForm form) throws ParseException {
//
//		formatConversion(form);
//
//		Integer lmsUserId = loginUserUtil.isStudent()
//				? loginUserDto.getLmsUserId()
//				: form.getLmsUserId();
//
//		List<TStudentAttendance> oldList = tStudentAttendanceMapper.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);
//
//		Date now = new Date();
//		List<TStudentAttendance> saveList = new ArrayList<>();
//
//		for (DailyAttendanceForm daily : form.getAttendanceList()) {
//
//			TStudentAttendance entity = new TStudentAttendance();
//			BeanUtils.copyProperties(daily, entity);
//
//			entity.setTrainingDate(dateUtil.parse(daily.getTrainingDate()));
//
//			TStudentAttendance oldEntity = oldList.stream()
//					.filter(o -> o.getTrainingDate().equals(entity.getTrainingDate()))
//					.findFirst()
//					.orElse(null);
//
//			if (oldEntity != null) {
//				entity = oldEntity;
//			}
//
//			entity.setLmsUserId(lmsUserId);
//			entity.setAccountId(loginUserDto.getAccountId());
//
//			if (daily.getTrainingStartTime() != null) {
//				entity.setTrainingStartTime(new TrainingTime(daily.getTrainingStartTime()).getFormattedString());
//			}
//
//			if (daily.getTrainingEndTime() != null) {
//				entity.setTrainingEndTime(new TrainingTime(daily.getTrainingEndTime()).getFormattedString());
//			}
//
//			entity.setBlankTime(daily.getBlankTime());
//
//			// --- 修正：Enumベースでステータス判定 ---
//			Short statusCode;
//			try {
//				statusCode = Short.valueOf(daily.getStatus());
//			} catch (Exception e) {
//				statusCode = AttendanceStatusEnum.NONE.code;
//			}
//
//			AttendanceStatusEnum currentStatusEnum = AttendanceStatusEnum.getEnum(statusCode);
//			boolean isAbsent = (currentStatusEnum == AttendanceStatusEnum.ABSENT);
//
//			if (!isAbsent) {
//
//				TrainingTime start = daily.getTrainingStartTime() != null
//						? new TrainingTime(daily.getTrainingStartTime())
//						: null;
//
//				TrainingTime end = daily.getTrainingEndTime() != null
//						? new TrainingTime(daily.getTrainingEndTime())
//						: null;
//
//				AttendanceStatusEnum statusEnum = attendanceUtil.getStatus(start, end);
//				if (statusEnum != null) {
//					entity.setStatus(statusEnum.code);
//				}
//
//			} else {
//				entity.setStatus(AttendanceStatusEnum.ABSENT.code);
//				entity.setTrainingStartTime(null);
//				entity.setTrainingEndTime(null);
//			}
//
//			entity.setNote(daily.getNote());
//			entity.setLastModifiedUser(loginUserDto.getLmsUserId());
//			entity.setLastModifiedDate(now);
//			entity.setDeleteFlg(Constants.DB_FLG_FALSE);
//
//			saveList.add(entity);
//		}
//
//		for (TStudentAttendance entity : saveList) {
//			if (entity.getStudentAttendanceId() == null) {
//				entity.setFirstCreateUser(loginUserDto.getLmsUserId());
//				entity.setFirstCreateDate(now);
//				tStudentAttendanceMapper.insert(entity);
//			} else {
//				tStudentAttendanceMapper.update(entity);
//			}
//		}
//
//		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
//	}
//}

package jp.co.sss.lms.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
	 */
	public boolean notEnterCheck() {
		// "yyyy-MM-dd"形式で日付のみを扱うフォーマットを準備
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// 現在日時（時刻あり）を取得
		Date now = new Date();
		// 現在日時を"yyyy-MM-dd"の文字列に変換
		String formattedToday = sdf.format(now);

		Date todayZeroTime;
		try {
			// 日付文字列をDateに変換
			todayZeroTime = sdf.parse(formattedToday);
		} catch (ParseException e) {
			// SimpleDateFormat#parse が投げる可能性がある唯一の例外
			// フォーマットが不正な場合に発生するが、format()で生成した文字列をparseしているため本来起こりえない
			throw new RuntimeException("日付フォーマット変換に失敗しました", e);
		}

		// DBに対して「今日より前の日付で、未入力の勤怠があるか」を問い合わせる
		// loginUserDto.getLmsUserId():ログイン中ユーザーのID
		// todayZeroTime              : 今日の0時（これより前の日付を検索対象とする）
		// Constants.DB_FLG_FALSE     : 未入力フラグ
		Integer count = tStudentAttendanceMapper.notEnterCount(
				loginUserDto.getLmsUserId(),
				todayZeroTime,
				Constants.DB_FLG_FALSE);

		// 未入力件数が1件以上ならtrueを返す
		return count > 0;
	}

	/**
	 * 勤怠一覧情報取得
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		List<AttendanceManagementDto> list = tStudentAttendanceMapper.getAttendanceManagement(courseId, lmsUserId,
				Constants.DB_FLG_FALSE);

		for (AttendanceManagementDto dto : list) {

			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}

			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return list;
	}

	/**
	 * 勤怠フォームへ設定（打刻済みは初期値セット、未入力は空欄）
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> dtoList) {

		AttendanceForm form = new AttendanceForm();
		form.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		form.setLmsUserId(loginUserDto.getLmsUserId());
		form.setUserName(loginUserDto.getUserName());
		form.setLeaveFlg(loginUserDto.getLeaveFlg());
		form.setBlankTimes(attendanceUtil.setBlankTime());

		// Task26：時・分の選択用マップ生成
		form.setStartHourMap(createHourMap());
		form.setEndHourMap(createHourMap());
		form.setStartMinuteMap(createMinuteMap());
		form.setEndMinuteMap(createMinuteMap());

		if (loginUserDto.getLeaveDate() != null) {
			form.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			form.setDispLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// DTO → DailyAttendanceForm
		for (AttendanceManagementDto dto : dtoList) {

			DailyAttendanceForm daily = new DailyAttendanceForm();
			daily.setStudentAttendanceId(dto.getStudentAttendanceId());
			daily.setTrainingDate(dateUtil.toString(dto.getTrainingDate()));
			daily.setTrainingStartTime(dto.getTrainingStartTime());
			daily.setTrainingEndTime(dto.getTrainingEndTime());

			// 出勤（hh:mm → 時・分）
			if (dto.getTrainingStartTime() != null && dto.getTrainingStartTime().length() == 5) {
				daily.setTrainingStartHour(dto.getTrainingStartTime().substring(0, 2));
				daily.setTrainingStartMinute(dto.getTrainingStartTime().substring(3, 5));
			} else {
				daily.setTrainingStartHour(null);
				daily.setTrainingStartMinute(null);
			}

			// 退勤（hh:mm → 時・分）
			if (dto.getTrainingEndTime() != null && dto.getTrainingEndTime().length() == 5) {
				daily.setTrainingEndHour(dto.getTrainingEndTime().substring(0, 2));
				daily.setTrainingEndMinute(dto.getTrainingEndTime().substring(3, 5));
			} else {
				daily.setTrainingEndHour(null);
				daily.setTrainingEndMinute(null);
			}

			if (dto.getBlankTime() != null) {
				daily.setBlankTime(dto.getBlankTime());
				daily.setBlankTimeValue(String.valueOf(attendanceUtil.calcBlankTime(dto.getBlankTime())));
			}

			daily.setStatus(String.valueOf(dto.getStatus()));
			daily.setNote(dto.getNote());
			daily.setSectionName(dto.getSectionName());
			daily.setIsToday(dto.getIsToday());
			daily.setDispTrainingDate(dateUtil.dateToString(dto.getTrainingDate(), "yyyy年M月d日(E)"));
			daily.setStatusDispName(dto.getStatusDispName());

			form.getAttendanceList().add(daily);
		}

		return form;
	}

	/** 時間マップ生成（00〜23） */
	private LinkedHashMap<Integer, String> createHourMap() {
		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
		for (int h = 0; h <= 23; h++) {
			map.put(h, String.format("%02d", h));
		}
		return map;
	}

	/** 分マップ生成（00,10,20,30,40,50） */
	private LinkedHashMap<Integer, String> createMinuteMap() {
		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
		for (int m = 0; m <= 50; m += 10) {
			map.put(m, String.format("%02d", m));
		}
		return map;
	}

	/**
	 * 時・分 → hh:mm（未入力は null）
	 */
	public void formatConversion(AttendanceForm form) {

		if (form.getAttendanceList() == null)
			return;

		for (DailyAttendanceForm daily : form.getAttendanceList()) {

			// 出勤
			if (daily.getTrainingStartHour() != null && daily.getTrainingStartMinute() != null) {
				daily.setTrainingStartTime(
						String.format("%02d:%02d",
								Integer.parseInt(daily.getTrainingStartHour()),
								Integer.parseInt(daily.getTrainingStartMinute())));
			} else {
				daily.setTrainingStartTime(null);
			}

			// 退勤
			if (daily.getTrainingEndHour() != null && daily.getTrainingEndMinute() != null) {
				daily.setTrainingEndTime(
						String.format("%02d:%02d",
								Integer.parseInt(daily.getTrainingEndHour()),
								Integer.parseInt(daily.getTrainingEndMinute())));
			} else {
				daily.setTrainingEndTime(null);
			}
		}
	}

	/**
	 * 勤怠登録・更新処理
	 */
	public String update(AttendanceForm form) throws ParseException {

		// 時・分 → hh:mm に変換
		formatConversion(form);

		Integer lmsUserId = loginUserUtil.isStudent()
				? loginUserDto.getLmsUserId()
				: form.getLmsUserId();

		List<TStudentAttendance> list = tStudentAttendanceMapper.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		Date now = new Date();

		for (DailyAttendanceForm daily : form.getAttendanceList()) {

			TStudentAttendance entity = new TStudentAttendance();
			BeanUtils.copyProperties(daily, entity);

			entity.setTrainingDate(dateUtil.parse(daily.getTrainingDate()));

			// 既存データがあれば上書き
			for (TStudentAttendance old : list) {
				if (old.getTrainingDate().equals(entity.getTrainingDate())) {
					entity = old;
					break;
				}
			}

			entity.setLmsUserId(lmsUserId);
			entity.setAccountId(loginUserDto.getAccountId());

			// 出勤
			if (daily.getTrainingStartTime() != null) {
				TrainingTime start = new TrainingTime(daily.getTrainingStartTime());
				entity.setTrainingStartTime(start.getFormattedString());
			} else {
				// 未入力 → 既存値を維持
				entity.setTrainingStartTime(entity.getTrainingStartTime());
			}

			// 退勤
			if (daily.getTrainingEndTime() != null) {
				TrainingTime end = new TrainingTime(daily.getTrainingEndTime());
				entity.setTrainingEndTime(end.getFormattedString());
			} else {
				// 未入力 → 既存値を維持
				entity.setTrainingEndTime(entity.getTrainingEndTime());
			}

			entity.setBlankTime(daily.getBlankTime());

			// ステータス判定（欠席以外）
			if (!"欠席".equals(daily.getStatusDispName())) {

				TrainingTime start = daily.getTrainingStartTime() != null
						? new TrainingTime(daily.getTrainingStartTime())
						: null;

				TrainingTime end = daily.getTrainingEndTime() != null
						? new TrainingTime(daily.getTrainingEndTime())
						: null;

				AttendanceStatusEnum statusEnum = attendanceUtil.getStatus(start, end);

				entity.setStatus(statusEnum.code);
			}

			entity.setNote(daily.getNote());
			entity.setLastModifiedUser(loginUserDto.getLmsUserId());
			entity.setLastModifiedDate(now);
			entity.setDeleteFlg(Constants.DB_FLG_FALSE);

			list.add(entity);
		}

		// 登録・更新
		for (TStudentAttendance entity : list) {
			if (entity.getStudentAttendanceId() == null) {
				entity.setFirstCreateUser(loginUserDto.getLmsUserId());
				entity.setFirstCreateDate(now);
				tStudentAttendanceMapper.insert(entity);
			} else {
				tStudentAttendanceMapper.update(entity);
			}
		}

		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

}
