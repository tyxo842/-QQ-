package com.xxx.qqslidingmenu.view;

import java.net.HttpURLConnection;

import org.apache.http.client.HttpClient;

import com.nineoldandroids.view.ViewHelper;
import com.xxx.qqslidingmenu.utils.EvaluateUtil;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v4.widget.ViewDragHelper.Callback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class DragLayout extends FrameLayout {

	protected static final String TAG = DragLayout.class.getSimpleName();

	private ViewDragHelper mDragHelper;
	private ViewGroup mLeftContent;
	private ViewGroup mMainContent;
	private int mWidth;
	private int mHeight;
	private int mDragRange;

	private OnDragStateChangeListener mOnDragStateChangeListener;

	private State mState = State.CLOSE;
	
	public enum State {
		CLOSE, OPEN, DRAGGING
	}

	public OnDragStateChangeListener getOnDragStateChangeListener() {
		return mOnDragStateChangeListener;
	}

	public void setOnDragStateChangeListener(
			OnDragStateChangeListener mOnDragStateChangeListener) {
		this.mOnDragStateChangeListener = mOnDragStateChangeListener;
	}

	public State getState() {
		return mState;
	}

	public void setState(State mState) {
		this.mState = mState;
	}

	// 代码new出来
	public DragLayout(Context context) {
		this(context, null);
	}

	// 在布局文件配置
	public DragLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);

	}

	// 这个构造方法外部不调用
	public DragLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		// forParent 要监视的父View
		// cb(callback) 提供信息, 接收事件
		mDragHelper = ViewDragHelper.create(this, mCallback);
	}

	public interface OnDragStateChangeListener {
		void onOpen();

		void onClose();

		void onDragging(float percent);
	}

	protected void open() {
		open(true);
	}

	public void open(boolean isSmooth) {
		int finalLeft = mDragRange;
		if (isSmooth) {
			mDragHelper.smoothSlideViewTo(mMainContent, finalLeft, 0);
			invalidate();
		} else {
			mMainContent.layout(finalLeft, 0, finalLeft + mWidth, mHeight);
		}
	}

	protected void close() {
		close(true);
	}

	public void close(boolean isSmooth) {
		int finalLeft = 0;
		if (isSmooth) {
			// "触发" 了一个平滑动画, 并且计算了第一帧
			mDragHelper.smoothSlideViewTo(mMainContent, finalLeft, 0);
			invalidate();
		} else {
			mMainContent.layout(finalLeft, 0, finalLeft + mWidth, mHeight);
		}
	}

	// cb(callback) 提供信息, 接收事件
	private Callback mCallback = new Callback() {

		// 当触摸到某个子View的时候, 这个方法会被调用, 返回值决定了是否捕获(处理)这个子View
		// 返回 true表示捕获, 返回false表示不捕获
		// child: 触摸到的子View
		// pointerId: 多点触摸的id, 用不到

		@Override
		public boolean tryCaptureView(View child, int pointerId) {
			// if(child == mMainContent) {
			// return true;
			// }else if(child == mLeftContent){
			// return false;
			// }

			return child == mMainContent || child == mLeftContent;
		}

		// 修正View水平方向的位置, 返回值决定View的水平方向位置
		// child: 捕获到的子View
		// left: ViewDragHelper建议的left值
		// dx: View当前的left值和建议值之间的差值
		public int clampViewPositionHorizontal(View child, int left, int dx) {
			Log.i(TAG, "clampViewPositionHorizontal---- left: " + left
					+ " dx: " + dx + " oldLeft: " + child.getLeft());
			if (child == mMainContent) {
				left = fixLeft(left);
			}
			return left;

		}

		private int fixLeft(int left) {
			if (left < 0) {
				left = 0;
			} else if (left > mDragRange) {
				left = mDragRange;
			}
			return left;
		};

		// 这个方法不影响水平方向的拖拽范围, 影响松手后的动画时长
		// 如果页面里有ListView等可以拖拽的控件, 这个方法必须返回大于0的值, 否则水平方向无法拖动
		public int getViewHorizontalDragRange(View child) {
			return mDragRange;
		};

		// 当View的位置发生改变时调用
		// dx: 移动之前的left值和移动之后left值的差值
		public void onViewPositionChanged(View changedView, int left, int top,
				int dx, int dy) {
			Log.i(TAG, "onViewPositionChanged---- left: " + left + " dx: " + dx
					+ " oldLeft: " + changedView.getLeft());
			if (changedView == mLeftContent) {
				mLeftContent.layout(0, 0, mWidth, mHeight);
				int newLeft = mMainContent.getLeft() + dx;
				newLeft = fixLeft(newLeft);
				mMainContent.layout(newLeft, 0, newLeft + mWidth, mHeight);
			}
			dispatchDragState(mMainContent.getLeft());
			invalidate();// 解决2.3.3 无法移动的bug
		};

		// 当View被释放的时候调用
		// xvel: 松手时x轴方向的速度, 向着x轴正方向松手, 值为正, 向着x轴负方向松手, 值为负
		public void onViewReleased(View releasedChild, float xvel, float yvel) {
			Log.i(TAG, "onViewReleased---- xvel:" + xvel);
			if (xvel == 0.0f && mMainContent.getLeft() < mDragRange * 0.5f) {
				close();
			} else if (xvel < 0) {
				close();
			} else {
				open();
			}
		};

	};

	protected void dispatchDragState(int left) {
		float percent = left * 1.0f / mDragRange;
		animViews(percent);
		State preState = mState;
		// 修改状态
		mState = updateState(percent);
		if (mOnDragStateChangeListener != null) {
			if (mState == State.DRAGGING) {
				mOnDragStateChangeListener.onDragging(percent);
			}
			// 状态改变的时候调用onClose(); onOpen();
			if (mState != preState) {
				if (mState == State.CLOSE) {
					mOnDragStateChangeListener.onClose();
				} else if (mState == State.OPEN) {
					mOnDragStateChangeListener.onOpen();
				}
			}
		}
	}

	/**
	 * 根据百分百,返回(修改)对应的状态;
	 */
	private State updateState(float percent) {
		if (percent == 0.0f) {
			return State.CLOSE;
		} else if (percent == 1.0f) {
			return State.OPEN;
		} else {
			return State.DRAGGING;
		}
	}

	private void animViews(float percent) {
		/**
		 * 1.5.1. 缩放(主面板, 左面板)
		 */
		// 1.0f - 0.8f --- percent 0.0f -1.0f
		// mMainContent.setScaleX(1.0f+(0.8f-1.0f)*percent);
		// mMainContent.setScaleY(1.0f+(0.8f-1.0f)*percent);
		ViewHelper.setScaleX(mMainContent, 1.0f + (0.8f - 1.0f) * percent);
		ViewHelper.setScaleY(mMainContent, 1.0f + (0.8f - 1.0f) * percent);
		// 0.5f - 1.0f
		// mLeftContent.setScaleX(0.5f + (1.0f-0.5f)*percent);
		// mLeftContent.setScaleY(0.5f + (1.0f-0.5f)*percent);
		ViewHelper.setScaleX(mLeftContent, 0.5f + (1.0f - 0.5f) * percent);
		ViewHelper.setScaleY(mLeftContent, 0.5f + (1.0f - 0.5f) * percent);
		/**
		 * 1.5.2. 平移(左面板) -mWidth*0.5f - 0
		 */
		ViewHelper.setTranslationX(mLeftContent,
				EvaluateUtil.evaluateFloat(percent, -mWidth * 0.5f, 0));
		/**
		 * 1.5.3. 透明度(左面板)
		 */
		ViewHelper.setAlpha(mLeftContent,
				EvaluateUtil.evaluateFloat(percent, 0.0f, 1.0f));

		/**
		 * 1.5.4. 亮度(背景)
		 */
		getBackground().setColorFilter(
				(Integer) EvaluateUtil.evaluateArgb(percent, Color.BLACK,
						Color.TRANSPARENT), Mode.SRC_OVER);
	}

	// 让ViewDragHelper决定是否拦截事件
	public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
		return mDragHelper.shouldInterceptTouchEvent(ev);
	};

	// 让ViewDragHelper处理触摸事件
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mDragHelper.processTouchEvent(event);
		return true;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		// 健壮性检查
		if (getChildCount() < 2) {
			throw new RuntimeException("You must have at least 2 child views");
		}
		if (!(getChildAt(0) instanceof ViewGroup)
				|| !(getChildAt(1) instanceof ViewGroup)) {
			throw new RuntimeException("Your child views must be ViewGroup");
		}
		mLeftContent = (ViewGroup) getChildAt(0);
		mMainContent = (ViewGroup) getChildAt(1);
	}

	// 在View的宽高发生变化的时候调用
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = mMainContent.getMeasuredWidth();
		mHeight = mMainContent.getMeasuredHeight();
		mDragRange = (int) (mWidth * 0.6f);
	}

	/**
	 * 判断是否结束,没有就继续调方法(见三个方法的循环图)
	 */
	@Override
	public void computeScroll() {
		super.computeScroll();
		if (mDragHelper.continueSettling(true)) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

}
