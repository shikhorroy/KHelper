package com.roy.khelper.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/** One place for the plugin's balloon notifications. The group id is declared in plugin.xml. */
object KHelperNotifications {
    private const val GROUP_ID = "KHelper Notifications"

    fun info(project: Project?, content: String, title: String = "KHelper") =
        notify(project, title, content, NotificationType.INFORMATION)

    fun warning(project: Project?, content: String, title: String = "KHelper") =
        notify(project, title, content, NotificationType.WARNING)

    fun error(project: Project?, content: String, title: String = "KHelper") =
        notify(project, title, content, NotificationType.ERROR)

    private fun notify(project: Project?, title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, type)
            .notify(project)
    }
}
