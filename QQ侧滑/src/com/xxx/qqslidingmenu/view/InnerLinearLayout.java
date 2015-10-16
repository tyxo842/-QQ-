package com.xxx.qqslidingmenu.view;

import com.xxx.qqslidingmenu.view.DragLayout.State;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class InnerLinearLayout extends LinearLayout {

	private DragLayout mDragLayout;

	/*
	 * public InnerLinearLayout(Context context, AttributeSet attrs, int
	 * defStyle) { super(context, attrs, defStyle); }
	 */

	public InnerLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public InnerLinearLayout(Context context) {
		super(context);
	}

	public void setDragLayout(DragLayout layout) {
		mDragLayout = layout;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (mDragLayout != null) {
			// 如果主面板状态为OPEN, 拦截事件
			if (mDragLayout.getState() == State.OPEN) {
				return true;
			}
		}
		return super.onInterceptTouchEvent(ev);

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mDragLayout != null) {
			if (mDragLayout.getState() == State.OPEN
					&& event.getAction() == MotionEvent.ACTION_UP) {
				mDragLayout.close(true);
			}
			// 如果想要处理 UP 事件, 得在这里返回true
			return true;
		}
		return super.onTouchEvent(event);
	}

}
