package com.day.antsschool.server.repository

import com.day.antsschool.server.domain.Chapter
import com.day.antsschool.server.domain.ChapterProgress
import com.day.antsschool.server.domain.EconomicQuote
import com.day.antsschool.server.domain.LearningCard
import com.day.antsschool.server.domain.NewsBookmark
import com.day.antsschool.server.domain.QuizQuestion
import com.day.antsschool.server.domain.UserBadge
import com.day.antsschool.server.domain.UserProgress
import com.day.antsschool.server.domain.WrongAnswer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface ChapterRepository : JpaRepository<Chapter, String> {
    fun findAllByOrderByGlobalOrderAsc(): List<Chapter>
}

interface CardRepository : JpaRepository<LearningCard, Long> {
    fun findByChapterIdOrderByCardOrderAsc(chapterId: String): List<LearningCard>
}

interface QuizRepository : JpaRepository<QuizQuestion, Long> {
    fun findByChapterIdOrderByQuestionOrderAsc(chapterId: String): List<QuizQuestion>
}

interface UserProgressRepository : JpaRepository<UserProgress, String>

interface ChapterProgressRepository : JpaRepository<ChapterProgress, Long> {
    fun findByUid(uid: String): List<ChapterProgress>
    fun findByUidAndChapterId(uid: String, chapterId: String): ChapterProgress?
}

interface UserBadgeRepository : JpaRepository<UserBadge, Long> {
    fun findByUid(uid: String): List<UserBadge>
    fun existsByUidAndBadgeId(uid: String, badgeId: String): Boolean
}

interface WrongAnswerRepository : JpaRepository<WrongAnswer, Long> {
    fun findByUid(uid: String): List<WrongAnswer>
    fun findByUidAndQuestionId(uid: String, questionId: Long): WrongAnswer?
}

interface EconomicQuoteRepository : JpaRepository<EconomicQuote, Long>

interface NewsBookmarkRepository : JpaRepository<NewsBookmark, Long> {
    fun findByUidOrderBySavedAtDesc(uid: String): List<NewsBookmark>
    fun existsByUidAndLink(uid: String, link: String): Boolean
    @Transactional
    fun deleteByUidAndLink(uid: String, link: String)
}
