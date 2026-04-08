package com.day.antsschool.server.api

import com.day.antsschool.server.api.dto.UserProgressResponse
import com.day.antsschool.server.domain.UserBadge
import com.day.antsschool.server.domain.UserProgress
import com.day.antsschool.server.repository.UserBadgeRepository
import com.day.antsschool.server.repository.UserProgressRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/progress")
class ProgressController(
    private val userProgressRepository: UserProgressRepository,
    private val userBadgeRepository: UserBadgeRepository
) {
    // 사용자 진도 조회
    @GetMapping("/{uid}")
    fun getProgress(@PathVariable uid: String): ResponseEntity<UserProgressResponse> {
        val progress = userProgressRepository.findById(uid).orElse(UserProgress(uid = uid))
        val badges = userBadgeRepository.findByUid(uid).map { it.badgeId }
        return ResponseEntity.ok(progress.toResponse(badges))
    }

    // 앱 시작 시 출석 체크 — 스트릭 업데이트 + STREAK_7 배지 지급
    @PostMapping("/{uid}/checkin")
    fun checkIn(@PathVariable uid: String): ResponseEntity<UserProgressResponse> {
        val today = LocalDate.now().toString()
        val current = userProgressRepository.findById(uid).orElse(UserProgress(uid = uid))

        // 오늘 이미 체크인했으면 스킵
        if (current.lastLoginDate == today) {
            val badges = userBadgeRepository.findByUid(uid).map { it.badgeId }
            return ResponseEntity.ok(current.toResponse(badges))
        }

        val newStreak = when (current.lastLoginDate) {
            LocalDate.now().minusDays(1).toString() -> current.streak + 1
            else -> 1
        }

        // 출석 날짜 목록 업데이트 (최근 90일만 보관)
        val dates = current.checkinDates.split("|").filter { it.isNotEmpty() }.toMutableList()
        if (today !in dates) dates.add(today)
        val cutoff = LocalDate.now().minusDays(90).toString()
        dates.removeAll { it < cutoff }
        val newCheckinDates = dates.joinToString("|")

        val updated = userProgressRepository.save(
            current.copy(streak = newStreak, lastLoginDate = today, checkinDates = newCheckinDates)
        )

        // 스트릭 배지
        if (newStreak >= 3 && !userBadgeRepository.existsByUidAndBadgeId(uid, "STREAK_3")) {
            userBadgeRepository.save(UserBadge(uid = uid, badgeId = "STREAK_3", earnedAt = today))
        }
        if (newStreak >= 7 && !userBadgeRepository.existsByUidAndBadgeId(uid, "STREAK_7")) {
            userBadgeRepository.save(UserBadge(uid = uid, badgeId = "STREAK_7", earnedAt = today))
        }
        if (newStreak >= 30 && !userBadgeRepository.existsByUidAndBadgeId(uid, "STREAK_30")) {
            userBadgeRepository.save(UserBadge(uid = uid, badgeId = "STREAK_30", earnedAt = today))
        }

        val badges = userBadgeRepository.findByUid(uid).map { it.badgeId }
        return ResponseEntity.ok(updated.toResponse(badges))
    }
}

private fun UserProgress.toResponse(badges: List<String>) = UserProgressResponse(
    uid = uid,
    level = level,
    xp = xp,
    xpForNextLevel = level * 100,
    streak = streak,
    badges = badges,
    checkinDates = checkinDates.split("|").filter { it.isNotEmpty() },
    totalCorrect = totalCorrect,
    totalAnswered = totalAnswered
)
