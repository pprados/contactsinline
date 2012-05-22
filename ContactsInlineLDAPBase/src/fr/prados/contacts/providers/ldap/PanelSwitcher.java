/*******************************************************************************
 * Copyright 2012 Philippe PRADOS 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package fr.prados.contacts.providers.ldap;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

public final class PanelSwitcher extends FrameLayout
{
	private static final int	ANIM_DURATION	= 400;
	private int					_currentView;
	private View				_children[]		= new View[0];
	private int					_width;
	private TranslateAnimation	_inLeft;
	private TranslateAnimation	_outLeft;
	private TranslateAnimation	_inRight;
	private TranslateAnimation	_outRight;

	public PanelSwitcher(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		_currentView = 0;
	}

	void setCurrentIndex(int current)
	{
		_currentView = current;
		updateCurrentView();
	}

	private void updateCurrentView()
	{
		for (int i = _children.length - 1; i >= 0; --i)
		{
			_children[i].setVisibility(i == _currentView ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void onSizeChanged(int w, int h, int oldW, int oldH)
	{
		_width = w;
		_inLeft = new TranslateAnimation(_width, 0, 0, 0);
		_outLeft = new TranslateAnimation(0, -_width, 0, 0);
		_inRight = new TranslateAnimation(-_width, 0, 0, 0);
		_outRight = new TranslateAnimation(0, _width, 0, 0);

		_inLeft.setDuration(ANIM_DURATION);
		_outLeft.setDuration(ANIM_DURATION);
		_inRight.setDuration(ANIM_DURATION);
		_outRight.setDuration(ANIM_DURATION);
	}

	@Override
	protected void onFinishInflate()
	{
		int count = getChildCount();
		_children = new View[count];
		for (int i = 0; i < count; ++i)
		{
			_children[i] = getChildAt(i);
		}
		updateCurrentView();
	}

	public void show(int currentView)
	{
		_children[_currentView].setVisibility(View.GONE);
		_children[currentView].setVisibility(View.VISIBLE);
		_currentView=currentView;
	}
	
	public void showNext()
	{
		showNext(_currentView+1);
	}
	public void showNext(int currentView)
	{
		// <--
		if (currentView < _children.length)
		{
			_children[currentView].setVisibility(View.VISIBLE);
			_children[currentView].startAnimation(_inLeft);
			_children[_currentView].startAnimation(_outLeft);
			_children[_currentView].setVisibility(View.GONE);

			_currentView=currentView;
		}
	}

	public void showPrevious()
	{
		showPrevious(_currentView-1);
	}
	public void showPrevious(int currentView)
	{
		// -->
		if (currentView >= 0)
		{
			_children[currentView].setVisibility(View.VISIBLE);
			_children[currentView].startAnimation(_inRight);
			_children[_currentView].startAnimation(_outRight);
			_children[_currentView].setVisibility(View.GONE);

			_currentView=currentView;
		}
	}

	public int getCurrentIndex()
	{
		return _currentView;
	}
	
}
