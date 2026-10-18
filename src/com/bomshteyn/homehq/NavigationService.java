package com.bomshteyn.homehq;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/* loaded from: classes.dex */
public class NavigationService extends AccessibilityService {
    private static boolean dashboardVisible;
    private static NavigationService instance;
    private boolean attached;
    private LinearLayout bar;
    private WindowManager.LayoutParams position;
    private WindowManager windows;

    static void setDashboardVisible(boolean z) {
        dashboardVisible = z;
        NavigationService navigationService = instance;
        if (navigationService != null) {
            navigationService.updateVisibility();
        }
    }

    @Override // android.accessibilityservice.AccessibilityService
    protected void onServiceConnected() {
        instance = this;
        this.windows = (WindowManager) getSystemService("window");
        LinearLayout linearLayout = new LinearLayout(this);
        this.bar = linearLayout;
        linearLayout.setGravity(16);
        this.bar.setPadding(dp(4), dp(4), dp(4), dp(4));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(Color.rgb(28, 28, 28));
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setStroke(dp(1), -7829368);
        this.bar.setBackground(gradientDrawable);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, 2032, 40, -3);
        this.position = layoutParams;
        layoutParams.gravity = 85;
        this.position.x = getSharedPreferences("kiosk", 0).getInt("nav_x", dp(12));
        this.position.y = getSharedPreferences("kiosk", 0).getInt("nav_y", dp(12));
        this.position.setTitle("Home HQ navigation");
        TextView textView = new TextView(this);
        textView.setText("⋮");
        textView.setTextColor(-3355444);
        textView.setTextSize(26.0f);
        textView.setGravity(17);
        textView.setContentDescription("Drag navigation buttons");
        this.bar.addView(textView, new LinearLayout.LayoutParams(dp(40), dp(48)));
        textView.setOnTouchListener(new View.OnTouchListener() { // from class: com.bomshteyn.homehq.NavigationService.1
            int oldX;
            int oldY;
            float startX;
            float startY;

            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == 0) {
                    this.startX = motionEvent.getRawX();
                    this.startY = motionEvent.getRawY();
                    this.oldX = NavigationService.this.position.x;
                    this.oldY = NavigationService.this.position.y;
                } else if (motionEvent.getActionMasked() == 2) {
                    int max = Math.max(0, NavigationService.this.getResources().getDisplayMetrics().widthPixels - NavigationService.this.bar.getWidth());
                    int max2 = Math.max(0, NavigationService.this.getResources().getDisplayMetrics().heightPixels - NavigationService.this.bar.getHeight());
                    NavigationService.this.position.x = Math.max(0, Math.min(max, this.oldX - ((int) (motionEvent.getRawX() - this.startX))));
                    NavigationService.this.position.y = Math.max(0, Math.min(max2, this.oldY - ((int) (motionEvent.getRawY() - this.startY))));
                    NavigationService.this.windows.updateViewLayout(NavigationService.this.bar, NavigationService.this.position);
                } else if (motionEvent.getActionMasked() == 1) {
                    NavigationService.this.getSharedPreferences("kiosk", 0).edit().putInt("nav_x", NavigationService.this.position.x).putInt("nav_y", NavigationService.this.position.y).apply();
                    view.performClick();
                }
                return true;
            }
        });
        addButton("Back", new View.OnClickListener() { // from class: com.bomshteyn.homehq.NavigationService$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                NavigationService.this.m6lambda$onServiceConnected$0$combomshteynhomehqNavigationService(view);
            }
        });
        addButton("Dashboard", new View.OnClickListener() { // from class: com.bomshteyn.homehq.NavigationService$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                NavigationService.this.m7lambda$onServiceConnected$1$combomshteynhomehqNavigationService(view);
            }
        });
        updateVisibility();
    }

    /* renamed from: lambda$onServiceConnected$0$com-bomshteyn-homehq-NavigationService, reason: not valid java name */
    /* synthetic */ void m6lambda$onServiceConnected$0$combomshteynhomehqNavigationService(View view) {
        performGlobalAction(1);
    }

    /* renamed from: lambda$onServiceConnected$1$com-bomshteyn-homehq-NavigationService, reason: not valid java name */
    /* synthetic */ void m7lambda$onServiceConnected$1$combomshteynhomehqNavigationService(View view) {
        startActivity(new Intent(this, (Class<?>) MainActivity.class).addFlags(335544320));
    }

    private void addButton(String str, View.OnClickListener onClickListener) {
        Button button = new Button(this);
        button.setText(str);
        button.setAllCaps(false);
        button.setTextColor(-1);
        button.setTextSize(17.0f);
        button.setBackgroundColor(0);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setOnClickListener(onClickListener);
        this.bar.addView(button, new LinearLayout.LayoutParams(-2, dp(48)));
    }

    private void updateVisibility() {
        LinearLayout linearLayout = this.bar;
        if (linearLayout == null) {
            return;
        }
        boolean z = dashboardVisible;
        if (!z && !this.attached) {
            this.windows.addView(linearLayout, this.position);
            this.attached = true;
        } else if (z && this.attached) {
            this.windows.removeView(linearLayout);
            this.attached = false;
        }
    }

    private int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onInterrupt() {
    }

    @Override // android.app.Service
    public void onDestroy() {
        if (this.attached) {
            this.windows.removeView(this.bar);
        }
        this.attached = false;
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }
}
