package nodec.bsssss.com.grabredpacketc.Service;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by shijg on 2018/1/4.
 */

public class GrabService extends AccessibilityService {
    private static final String TAG = "Plus";
    private static final String FLOW = "flow";

    private int serviceState = -1;

    private static final int FLAG_BACKGROUND = 100;
    private static final int FLAG_CHAT_LIST = 200;
    private static final int FLAG_CHAT_DETAIL = 300;
    private static final int FLAG_PACKET_RECEIVE = 500;
    private static final int FLAG_PACKET_DETAIL = 600;
    private static final int FLAG_IGNORE = 400;

    private int behavior = -1;

    private static final int FLAG_CAUGHT = 500;
    private static final int FLAG_FOUND = 900;
    private static final int FLAG_OPENING = 600;
    private static final int FLAG_OPENED = 700;
    private static final int FLAG_INS = 800;

    private Timer timer = null;

    private int start = -1;

    private static final int START_BACKGROUND = 100;
    private static final int START_LIST = 200;
    private static final int START_DETAIL = 300;

    private boolean back = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        Log.d(TAG,"eventType: " + eventType + "  serviceState: " + serviceState + " behavior: " + behavior + " start: " + start);
        String className = event.getClassName().toString();
        Log.d(TAG,className);
        if (back)return;
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:// 监听微信通知
                handleNotification(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (start == START_BACKGROUND && serviceState == FLAG_BACKGROUND && behavior == FLAG_FOUND){// 兼容一些设备
                    AccessibilityNodeInfo node = getPacket();
                    while (node != null){
                        if (node.isClickable()){
                            serviceState = FLAG_CHAT_DETAIL;
                            start = START_BACKGROUND;// 从后台唤醒标识
                            behavior = FLAG_CAUGHT;
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                        node = node.getParent();
                    }
                    return;
                }
                // 拆开红包界面
                if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(className)){
//                    findATarget = 0;
                    // TODO
                    timer = new Timer(true);
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            openPacket(getRootInActiveWindow());
                            behavior = FLAG_OPENED;
                            serviceState = FLAG_PACKET_DETAIL;
                        }
                    },300);
                    return;
                }
                // 红包详情界面
//                if (serviceState == FLAG_PACKET_DETAIL && behavior == FLAG_OPENED && "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(className) ){
                if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(className) ){
                    close(getRootInActiveWindow());
                    back = true;
                    serviceState = FLAG_CHAT_DETAIL;
                    behavior = FLAG_INS;
                    if (start == START_BACKGROUND || start == START_LIST){
                        timer = new Timer(true);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                close(getRootInActiveWindow());
                                serviceState = FLAG_CHAT_LIST;
                                back = false;
                                timer.cancel();
                            }
                        },400);
                    }else {
                        back = false;
                    }
                    return;
                }
                break;

            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                // 监控从后台唤醒微信的行为
//                if ((serviceState == FLAG_BACKGROUND) && className.equals("android.widget.FrameLayout") && start == START_BACKGROUND){
////                    serviceState = FLAG_CHAT_LIST;
//                    frameLayoutCount++;
//                    Log.d(FLOW,"3 " + start);
//                    if (frameLayoutCount == 2){
//                        frameLayoutCount = 0;
//
//                        AccessibilityNodeInfo node = getPacket();
//                        while (node != null){
//                            if (node.isClickable()){
//                                serviceState = FLAG_CHAT_DETAIL;
//                                start = START_BACKGROUND;// 从后台唤醒标识
//                                behavior = FLAG_CAUGHT;
//                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                                break;
//                            }
//                            node = node.getParent();
//                        }
//                    }
//                    return;
//                }
                // 未发现红包时监听聊天列表
                if (behavior == FLAG_INS && className.equals("android.widget.ImageView")){
//                if (serviceState == FLAG_CHAT_LIST && behavior == FLAG_INS && className.equals("android.widget.ImageView")){
                    AccessibilityNodeInfo node = recycleContains(getRootInActiveWindow());
                    if(node != null){
                        while (node != null){
                            if (node.isClickable()){
                                serviceState = FLAG_CHAT_DETAIL;
                                start = START_LIST;
                                behavior = FLAG_FOUND;
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                break;
                            }
                            node = node.getParent();
                        }
                        return;
                    }else {
                        // 监听聊天详情页面
                        AccessibilityNodeInfo node1 = getPacket();
                        while (node1 != null){
                            if (node1.isClickable()){
                                serviceState = FLAG_CHAT_DETAIL;
                                start = START_DETAIL;
                                behavior = FLAG_FOUND;
                                node1.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                break;
                            }
                            node1 = node1.getParent();
                        }
                        return;
                    }

                }
//                // 打开在列表中发现的红包
                if (serviceState == FLAG_CHAT_DETAIL && start == START_LIST && className.equals("android.widget.ImageView") && behavior == FLAG_FOUND){
                    AccessibilityNodeInfo node = getPacket();
                    while (node != null){
                        if (node.isClickable()){
                            serviceState = FLAG_CHAT_DETAIL;
                            start = START_LIST;
                            behavior = FLAG_CAUGHT;
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                        node = node.getParent();
                    }
                    return;
                }
                break;
        }
    }

    @Override
    public void onInterrupt() {

    }

    public void handleNotification(AccessibilityEvent event){
        List<CharSequence> texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                //如果微信红包的提示信息,则模拟点击进入相应的聊天窗口
                if (content.contains("[微信红包]")) {
                    if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                        Notification notification = (Notification) event.getParcelableData();
                        PendingIntent pendingIntent = notification.contentIntent;
                        try {
                            behavior = FLAG_FOUND;
                            serviceState = FLAG_BACKGROUND;
                            start = START_BACKGROUND;
                            pendingIntent.send();
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(getApplicationContext(),"服务成功开启",Toast.LENGTH_SHORT).show();
    }

    private void close(AccessibilityNodeInfo rootNode) {
        AccessibilityNodeInfo closeNode = getCloseNode(rootNode);
        if (closeNode != null) {
            closeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            closeNode.recycle();
        }
    }

    /**
     * 模拟点击,拆开红包
     */
    private void openPacket(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            AccessibilityNodeInfo node = getOpenButton(nodeInfo);
            if (node != null){
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                node.recycle();
            }
            nodeInfo.recycle();
        }
    }

    private AccessibilityNodeInfo getPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        return recycle(rootNode,"领取红包");
    }

    private boolean checkPacketFromList(AccessibilityNodeInfo rootNode){
        Log.d(TAG,"checkPacketFromList");

        return false;
    }

    public AccessibilityNodeInfo recycleContains(AccessibilityNodeInfo node){
        if (node.getChildCount() == 0){
            if (node.getText() != null){
                if (node.getText().toString().contains("[微信红包]")){
                    return node;
                }
            }
        }else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    AccessibilityNodeInfo agent = recycleContains(node.getChild(i));
                    if (agent != null)return agent;
                }
            }
        }
        return null;
    }

    public AccessibilityNodeInfo recycle(AccessibilityNodeInfo node,String text) {
        if (node.getChildCount() == 0) {
            if (node.getText() != null) {
                if (text.equals(node.getText().toString())) {
                    return node;
                }
            }
        } else {
            for (int i = node.getChildCount() - 1; i > -1; i--) {
                if (node.getChild(i) != null) {
                    AccessibilityNodeInfo agent = recycle(node.getChild(i),text);
                    if (agent != null)return agent;
                }
            }
        }
        return null;
    }

    private AccessibilityNodeInfo getOpenButton(AccessibilityNodeInfo node){
        if (node.getChildCount() == 0) {
            if ("android.widget.Button".equals(node.getClassName().toString())) {
                if (node.getText() != null)Log.d(FLOW,node.getText().toString());
                return node;
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    AccessibilityNodeInfo agent = getOpenButton(node.getChild(i));
                    if (agent != null)return agent;
                }
            }
        }
        return null;
    }

    private AccessibilityNodeInfo getCloseNode(AccessibilityNodeInfo node){
        if (node.getChildCount() > 0){
            if ("android.widget.LinearLayout".equals(node.getClassName().toString()) && node.isClickable() && hasImageViewChild(node)){
                return node;
            }else {
                for (int i = 0; i < node.getChildCount(); i++) {
                    AccessibilityNodeInfo agent = getCloseNode(node.getChild(i));
                    if (agent != null)return agent;
                }
            }
        }
        return null;
    }
    private boolean hasImageViewChild(AccessibilityNodeInfo node){
        return node.getChildCount() > 0 && "android.widget.ImageView".equals(node.getChild(0).getClassName().toString());
    }
}
