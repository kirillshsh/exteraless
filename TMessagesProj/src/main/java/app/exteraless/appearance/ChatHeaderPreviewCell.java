package app.exteraless.appearance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;

/**
 * Превью шапки открытого чата для экрана «Оформление». Собрано по образцу
 * {@link ChatListPreviewCell}: внутри не рисованный макет, а настоящие
 * {@link ActionBar} и {@link ChatAvatarContainer} — превью показывает ровно то,
 * что нарисует чат, включая стеклянную капсулу и счётчик у кнопки «Назад».
 *
 * Данные берутся из собственного аккаунта: своё имя, свой аватар и «online».
 */
@SuppressLint("ViewConstructor")
public class ChatHeaderPreviewCell extends FrameLayout {

    /** Счётчик непрочитанных в превью фиксированный: настоящий может быть и нулём. */
    private static final int PREVIEW_UNREAD = 10;

    private final BaseFragment fragment;
    private final ActionBar actionBar;
    private final BlurredBackgroundSourceColor backgroundSource;

    private ChatAvatarContainer avatarContainer;

    public ChatHeaderPreviewCell(Context context, BaseFragment fragment) {
        super(context);
        this.fragment = fragment;
        setWillNotDraw(false);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        actionBar = new ActionBar(context);
        actionBar.setOccupyStatusBar(false);
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.createMenu().addItem(0, R.drawable.ic_ab_other);

        // Стекло без настоящего блюр-источника: подложка превью — сплошной цвет.
        // Тот же приём, что в FoldersPreviewCell.
        backgroundSource = new BlurredBackgroundSourceColor();
        backgroundSource.setColor(PreviewColors.getBackgroundColor());
        actionBar.setupGlass(new BlurredBackgroundDrawableViewFactory(backgroundSource),
                BlurredBackgroundProviderImpl.topPanelChatActivity(null), false);
        ChatHeaderUiHelper.applyChatHeaderGlassStyle(actionBar);
        // setupGlass снимает фон, поэтому карточка ставится после него.
        actionBar.setBackground(new PreviewBackgroundDrawable());

        addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER, 21, 21, 21, 21));

        rebuildAvatarContainer();
        applyColors();
    }

    /**
     * Центровка, гравитация заголовка и видимость иконок читаются
     * {@link ChatAvatarContainer} один раз при создании, поэтому на смену настроек
     * контейнер пересоздаётся целиком — дешевле и надёжнее, чем повторять его
     * внутреннюю раскладку снаружи.
     */
    public void update() {
        rebuildAvatarContainer();
        applyColors();
        invalidate();
    }

    private void rebuildAvatarContainer() {
        if (avatarContainer != null) {
            actionBar.removeView(avatarContainer);
        }
        avatarContainer = new ChatAvatarContainer(getContext(), fragment, false) {
            @Override
            protected boolean isCentered() {
                return ChatHeaderUiHelper.isChatTitleCentered();
            }

            @Override
            protected boolean isPreviewMode() {
                return true;
            }
        };
        ChatHeaderUiHelper.setupGlassAvatarContainer(avatarContainer);
        avatarContainer.setOccupyStatusBar(false);
        avatarContainer.setClipChildren(false);
        actionBar.addView(avatarContainer, 0, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT,
                LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 52, 0, 52, 0));
        actionBar.createMenu().bringToFront();

        // Ровно то же условие, что в ChatActivity: контейнер отдаётся ActionBar только
        // когда капсула должна обжиматься по ширине заголовка.
        if (AppearanceConfig.adaptiveHeaderBubble()) {
            actionBar.setChatAvatarContainer(avatarContainer);
            avatarContainer.setActionBar(actionBar);
        } else {
            actionBar.setChatAvatarContainer(null);
        }

        final int account = UserConfig.selectedAccount;
        final TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        if (user != null) {
            avatarContainer.setTitle(UserObject.getUserName(user));
            avatarContainer.setUserAvatar(user, true);
        }
        avatarContainer.setSubtitle(LocaleController.getString(R.string.Online));

        actionBar.unreadBadgeSetCount(PREVIEW_UNREAD);
    }

    private void applyColors() {
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        backgroundSource.setColor(PreviewColors.getBackgroundColor());
        final int itemsColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
        actionBar.setItemsColor(itemsColor, false);
        if (avatarContainer != null) {
            avatarContainer.setTitleColors(itemsColor, Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (actionBar != null) {
            // Цвета выставляются один раз при сборке, поэтому смену темы доносим руками —
            // экран зовёт invalidate() в onResume.
            applyColors();
            actionBar.invalidate();
        }
    }
}
