package com.axelby.podax.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.axelby.podax.R;

public class RangeSeekBar extends View {
	private TextPaint _textPaint;
	private float _maxTextWidth = 0;
	private float _endTextStart;
	private float _textHeight;
	private float _textY;

	private boolean _control1Pressed = false;
	private boolean _control2Pressed = false;
	private Rect _control1 = new Rect();
	private Rect _control2 = new Rect();

	private Drawable _bg;
	private Drawable _bgActive;
	private Drawable _bgDisabled;
	private Drawable _control;
	private Drawable _controlPressed;
	private Drawable _controlDisabled;

	// state
	private CharSequence[] _entries;
	private int _minEntry = 0;
	private int _maxEntry = 1;
	private int _dragging = 0;

	private OnRangeSeekBarChangeListener _changeListener;
	public interface OnRangeSeekBarChangeListener {
		void onValuesChanged(RangeSeekBar rangeSeekBar, int minValue, int maxValue);
	}
	public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener l) {
		_changeListener = l;
	}

	public RangeSeekBar(Context context) {
		super(context);
		init(null, 0);
	}

	public RangeSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	public RangeSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	private void init(AttributeSet attrs, int defStyle) {
		// cache drawables
		Resources resources = getContext().getResources();
		_bg = resources.getDrawable(R.drawable.scrubber_track_holo_dark);
		_bgActive = resources.getDrawable(R.drawable.scrubber_primary_holo);
		_bgDisabled = resources.getDrawable(R.drawable.scrubber_secondary_holo);

		_control = resources.getDrawable(R.drawable.scrubber_control_normal_holo);
		_controlPressed = resources.getDrawable(R.drawable.scrubber_control_pressed_holo);
		_controlDisabled = resources.getDrawable(R.drawable.scrubber_control_disabled_holo);

		// set up text paint
		_textPaint = new TextPaint();
		_textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		_textPaint.setTextAlign(Paint.Align.LEFT);
		_textPaint.setColor(getResources().getColor(R.color.primary_text_default_material_dark));
		_textPaint.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);

		// read xml attributes
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar, defStyle, 0);
		int textSize = a.getDimensionPixelOffset(R.styleable.RangeSeekBar_textSize, 0);
		if (textSize != 0)
			_textPaint.setTextSize(textSize);
		_entries = a.getTextArray(R.styleable.RangeSeekBar_entries);
		_maxEntry = _entries.length - 1;
		a.recycle();

		if (_entries == null)
			throw new IllegalArgumentException("entries cannot be null");

		// calculate text sizes
		for (CharSequence c : _entries)
			_maxTextWidth = Math.max(_maxTextWidth, _textPaint.measureText(c.toString()) + 0.5f);
		Paint.FontMetrics fontMetrics = _textPaint.getFontMetrics();
		_textHeight = fontMetrics.bottom - fontMetrics.top;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = (int) (_maxTextWidth * 2 + 50);
		int h = (int) Math.max(_control.getIntrinsicHeight(), _textHeight);

		int wSpecMode = MeasureSpec.getMode(widthMeasureSpec);
		int wSpecSize = MeasureSpec.getSize(widthMeasureSpec);
		if (wSpecMode == MeasureSpec.EXACTLY)
			w = wSpecSize;
		else if (getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT)
			w = wSpecSize;
		else if (wSpecMode == MeasureSpec.AT_MOST)
			w = Math.min(w, wSpecSize);


		int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
		int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);
		if (hSpecMode == MeasureSpec.EXACTLY)
			h = hSpecSize;
		else if (getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT)
			h = hSpecSize;
		else if (hSpecMode == MeasureSpec.AT_MOST)
			h = Math.min(h, hSpecSize);

		setMeasuredDimension(w, h);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		recalcStaticPositions(w, h);
		recalcControlPositions(w, h);
	}

	private void recalcStaticPositions(int w, int h) {
		int textPadding = 10 + _control.getIntrinsicWidth() / 2;
		int left = (int) (getPaddingLeft() + _maxTextWidth + textPadding);
		int top = getPaddingTop();
		int right = (int) (w - getPaddingLeft() - getPaddingRight() - _maxTextWidth - textPadding * 2);
		int bottom = h - getPaddingTop() - getPaddingBottom();

		_bg.setBounds(left, top, right, bottom);

		Rect textRect = new Rect();
		_textPaint.getTextBounds("Aa", 0, 1, textRect);
		_textY = (h - getPaddingTop() - getPaddingBottom()) / 2 - textRect.exactCenterY();
	}

	private void recalcControlPositions(int w, int h) {
		// prepare bounds
		int textPadding = 10 + _control.getIntrinsicWidth() / 2;
		int left = (int) (getPaddingLeft() + _maxTextWidth + textPadding);
		int top = getPaddingTop();
		int right = (int) (w - getPaddingLeft() - getPaddingRight() - _maxTextWidth - textPadding * 2);
		int bottom = h - getPaddingTop() - getPaddingBottom();
		int activeWidth = right - left;

		// calculate control positions
		int c1X = left + activeWidth * _minEntry / (_entries.length-1);
		int c2X = left + activeWidth * _maxEntry / (_entries.length-1);
		int cWidth = _control.getIntrinsicWidth() / 2;

		_endTextStart = w - getPaddingRight() - _maxTextWidth;

		_bgActive.setBounds(c1X, top, c2X, bottom);
		_bgDisabled.setBounds(c1X, top, c2X, bottom);
		_control1.set(c1X - cWidth, top, c1X + cWidth, bottom);
		_control2.set(c2X - cWidth, top, c2X + cWidth, bottom);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		_bg.draw(canvas);
		if (isEnabled())
			_bgActive.draw(canvas);
		else
			_bgDisabled.draw(canvas);

		Drawable c = !isEnabled() ? _controlDisabled : _control1Pressed ? _controlPressed : _control;
		c.setBounds(_control1);
		c.draw(canvas);
		c = !isEnabled() ? _controlDisabled : _control2Pressed ? _controlPressed : _control;
		c.setBounds(_control2);
		c.draw(canvas);

		canvas.drawText(_entries[_minEntry].toString(), getPaddingLeft(), _textY, _textPaint);
		canvas.drawText(_entries[_maxEntry].toString(), _endTextStart, _textY, _textPaint);
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		if (!isEnabled())
			return true;

		final int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				if (_control1.contains((int) event.getX(), (int) event.getY() + 1)) {
					_control1Pressed = true;
					_dragging = 1;
				} else if (_control2.contains((int) event.getX(), (int) event.getY() + 1)) {
					_control2Pressed = true;
					_dragging = 2;
				} else {
					_dragging = 0;
					break;
				}

				invalidate();

				// attempt to prevent parent from stealing touch events
				if (getParent() != null)
					getParent().requestDisallowInterceptTouchEvent(true);

				break;
			case MotionEvent.ACTION_MOVE:
				if (_dragging == 0)
					break;

				// find which entry is under touch
				// 0----1----2----3----4
				// 0---25---50---75--100
				Rect bgBounds = _bg.getBounds();
				float xPct = (event.getX() - bgBounds.left) / bgBounds.width();
				// adjust windows so they center around the entry
				xPct -= 1.0f / _entries.length / 2;
				int entry = Math.round(_entries.length * xPct);

				int oldMinEntry = _minEntry;
				int oldMaxEntry = _maxEntry;

				if (_dragging == 1 && _minEntry != entry) {
					_minEntry = entry;
				} else if (_dragging == 2 && _maxEntry != entry) {
					_maxEntry = entry;
				} else {
					break;
				}

				if (_minEntry < 0)
					_minEntry = 0;
				if (_maxEntry >= _entries.length)
					_maxEntry = _entries.length - 1;
				if (_minEntry >= _maxEntry) {
					if (_dragging == 2)
						_maxEntry = _minEntry + 1;
					else
						_minEntry = _maxEntry - 1;
				}

				if (oldMinEntry == _minEntry && oldMaxEntry == _maxEntry)
					break;

				recalcControlPositions(getWidth(), getHeight());
				invalidate();
				if (_changeListener != null)
					_changeListener.onValuesChanged(this, _minEntry, _maxEntry);

				break;
			case MotionEvent.ACTION_UP:
				_control1Pressed = false;
				_control2Pressed = false;
				invalidate();
				break;
			case MotionEvent.ACTION_CANCEL:
				_control1Pressed = false;
				_control2Pressed = false;
				invalidate();
		}
		return true;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putParcelable("super", super.onSaveInstanceState());
		bundle.putInt("minEntry", _minEntry);
		bundle.putInt("maxEntry", _maxEntry);
		return bundle;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable parcel) {
		Bundle bundle = (Bundle) parcel;
		super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
		_minEntry = bundle.getInt("minEntry");
		_maxEntry = bundle.getInt("maxEntry");
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
	}
}
