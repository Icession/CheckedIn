// format.js — turns the backend's codes and times into words for the screen.
// Shared by several components, so it lives here instead of in one of them.

export const SEMESTER_LABELS = { FIRST: 'First Semester', SECOND: 'Second Semester', SUMMER: 'Summer' }

export const SESSION_TYPE_LABELS = { LECTURE: 'Lecture', LABORATORY: 'Laboratory' }

export const DELIVERY_MODE_LABELS = { IN_PERSON: 'In person', ONLINE: 'Online', HYBRID: 'Hybrid', FIELD: 'Field' }

/** FR-015: in-person and hybrid sessions are held in a room with a layout; online and field ones are not. */
export function usesLayout(deliveryMode) {
  return deliveryMode === 'IN_PERSON' || deliveryMode === 'HYBRID'
}

// Classes are Monday to Saturday (the backend refuses Sunday)
export const SCHOOL_DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

export const DAY_LABELS = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
}

/** "18:00:00" (from the backend) or "18:00" (from a time input) -> "6:00 PM". */
export function formatTime(time) {
  const [hourText, minuteText] = time.split(':')
  const hour = Number(hourText)
  const suffix = hour < 12 ? 'AM' : 'PM'
  const hourOnClock = hour % 12 === 0 ? 12 : hour % 12
  return `${hourOnClock}:${minuteText} ${suffix}`
}

/** "6:00 PM – 9:00 PM" */
export function formatTimeRange(startTime, endTime) {
  return `${formatTime(startTime)} – ${formatTime(endTime)}`
}

/** 3 -> "3 hours", 1.5 -> "1.5 hours", 1 -> "1 hour" (FR-014). */
export function formatHours(hours) {
  const rounded = Math.round(hours * 100) / 100
  return `${rounded} ${rounded === 1 ? 'hour' : 'hours'}`
}

/** The school year a new section most likely belongs to, e.g. "2026-2027" (it starts mid-year). */
export function currentSchoolYear(today = new Date()) {
  const year = today.getFullYear()
  const startsThisYear = today.getMonth() >= 5 // June or later
  return startsThisYear ? `${year}-${year + 1}` : `${year - 1}-${year}`
}

// Times are shown in Philippine time (NFR-035), whatever the computer's own time zone
const PHILIPPINE_TIME = { timeZone: 'Asia/Manila' }

/** "11:40 AM" for a time today, "Nov 18, 11:40 AM" for another day (e.g. when unsaved work was kept). */
export function formatTimeOfDay(isoTime, now = new Date()) {
  const time = new Date(isoTime)
  const clock = time.toLocaleTimeString('en-US', { ...PHILIPPINE_TIME, hour: 'numeric', minute: '2-digit' })
  const day = (date) => date.toLocaleDateString('en-US', PHILIPPINE_TIME)
  if (day(time) === day(now)) return clock
  return `${time.toLocaleDateString('en-US', { ...PHILIPPINE_TIME, month: 'short', day: 'numeric' })}, ${clock}`
}
