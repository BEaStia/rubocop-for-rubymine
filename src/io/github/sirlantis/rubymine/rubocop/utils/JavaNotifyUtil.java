package io.github.sirlantis.rubymine.rubocop.utils;

import com.intellij.notification.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import io.github.sirlantis.rubymine.rubocop.JavaRubocopBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.HashMap;

/**
 * Created by igorpavlov on 19.08.16.
 */
public class JavaNotifyUtil {

    static class JavaListener  implements NotificationListener {
        @Override
        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
            if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                Logger logger = Logger.getInstance(JavaRubocopBundle.LOG_ID);
                logger.warn("TODO something");
            }
        }
    }

    private static NotificationGroup TOOLWINDOW_NOTIFICATION = NotificationGroup.toolWindowGroup("RuboCop Errors", ToolWindowId.MESSAGES_WINDOW, true);
    private static NotificationGroup STICKY_NOTIFICATION = new NotificationGroup("Sticky RuboCop Errors", NotificationDisplayType.STICKY_BALLOON, true);
    private static NotificationGroup BALLOON_NOTIFICATION = new NotificationGroup("RuboCop Notifications", NotificationDisplayType.BALLOON, true);
    private static HashMap<NotificationGroup, Notification> SHOWN_NOTIFICATIONS = new HashMap<>();

    public static void notifySuccess(Project project, String title, String message) {
        notify(NotificationType.INFORMATION, BALLOON_NOTIFICATION, project, title, message);
    }

    public static void notifyInfo(Project project, String title, String message) {
        notify(NotificationType.INFORMATION, TOOLWINDOW_NOTIFICATION, project, title, message);
    }

    public static void notifyError(Project project, String title, String message) {
        notify(NotificationType.ERROR, TOOLWINDOW_NOTIFICATION, project, title, message);
    }

    public static void notifyError(Project project, String title, Exception exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getCanonicalName();
        notify(NotificationType.ERROR, STICKY_NOTIFICATION, project, title, message);
    }

    private static void notify(NotificationType notificationType, NotificationGroup group, Project project, String title, String message) {
        SHOWN_NOTIFICATIONS.get(group).expire();

        JavaListener listener = new JavaListener();
        Notification notification = group.createNotification(title, message, notificationType, listener);

        notification.whenExpired(()->{
            if (SHOWN_NOTIFICATIONS.get(group) == notification) {
                SHOWN_NOTIFICATIONS.remove(group);
            }
        });

        SHOWN_NOTIFICATIONS.put(group, notification);
        notification.notify(project);
    }
}
