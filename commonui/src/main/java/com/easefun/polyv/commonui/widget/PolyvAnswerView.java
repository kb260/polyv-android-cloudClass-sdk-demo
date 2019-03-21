package com.easefun.polyv.commonui.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.easefun.polyv.cloudclass.PolyvSocketEvent;
import com.easefun.polyv.cloudclass.model.PolyvSocketMessageVO;
import com.easefun.polyv.cloudclass.model.answer.PolyvQuestionResultVO;
import com.easefun.polyv.cloudclass.model.answer.PolyvQuestionSResult;
import com.easefun.polyv.cloudclass.video.PolyvAnswerWebView;
import com.easefun.polyv.commonui.R;
import com.easefun.polyv.foundationsdk.rx.PolyvRxBus;
import com.easefun.polyv.foundationsdk.utils.PolyvGsonUtil;
import com.easefun.polyv.foundationsdk.utils.PolyvScreenUtils;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.easefun.polyv.cloudclass.PolyvSocketEvent.TEST_QUESTION;

/**
 * 答题
 *
 * @author df
 * @create 2018/9/3
 * @Describe
 */
public class PolyvAnswerView extends FrameLayout {

    private static final String TAG = "PolyvAnswerView";

    private PolyvAnswerWebView answerWebView;
    private ViewGroup answerContainer;
    private ImageView close;
    private Disposable messageDispose;

    public PolyvAnswerView(@NonNull Context context) {
        this(context, null);
    }

    public PolyvAnswerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PolyvAnswerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialView(context);
    }

    private void initialView(Context context) {
        View.inflate(context, R.layout.polyv_answer_web_layout, this);
        answerWebView = findViewById(R.id.polyv_question_web);
        answerContainer = findViewById(R.id.polyv_answer_web_container);
        close = findViewById(R.id.answer_close);
        answerWebView.loadUrl("file:///android_asset/index.html");
        messageDispose = PolyvRxBus.get().toObservable(PolyvSocketMessageVO.class).subscribe(new Consumer<PolyvSocketMessageVO>() {
            @Override
            public void accept(PolyvSocketMessageVO polyvSocketMessage) throws Exception {

                String event = polyvSocketMessage.getEvent();
                processSocketMessage(polyvSocketMessage, event);
            }
        });

        close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                answerContainer.setVisibility(GONE);
            }
        });
    }

    public void processSocketMessage(PolyvSocketMessageVO polyvSocketMessage, String event) {
        String msg = polyvSocketMessage.getMessage();
        if (msg == null || event == null) {
            return;
        }
        switch (event) {
            //讲师发题
            case PolyvSocketEvent.GET_TEST_QUESTION_CONTENT:
                PolyvQuestionSResult polyvQuestionSResult = PolyvGsonUtil.fromJson(PolyvQuestionSResult.class, polyvSocketMessage.getMessage());
                if (polyvQuestionSResult != null && "S".equals(polyvQuestionSResult.getType())) {
                    break;
                }
                showAnswerContainer();
                answerWebView.callUpdateNewQuestion(msg);
                break;
            //讲师发送答题结果
            case PolyvSocketEvent.GET_TEST_QUESTION_RESULT:
                PolyvQuestionResultVO socketVO;
                socketVO = PolyvGsonUtil.fromJson(PolyvQuestionResultVO.class, polyvSocketMessage.getMessage());
                if (socketVO == null) {
                    return;
                }
                if (socketVO.getResult() != null && "S".equals(socketVO.getResult().getType())) {
                    return;
                }
                showAnswerContainer();
                answerWebView.callHasChooseAnswer(socketVO.getQuestionId(), polyvSocketMessage.getMessage());
                break;
            //截止答题
            case PolyvSocketEvent.STOP_TEST_QUESTION:
                answerWebView.callStopQuestion();
                break;
            //其他
            default:
                if (event.contains(TEST_QUESTION)) {
                    answerWebView.callTestQuestion(polyvSocketMessage.getMessage());
                }
                break;
        }
    }

    private void showAnswerContainer() {
        answerContainer.setVisibility(VISIBLE);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (messageDispose != null && !messageDispose.isDisposed()) {
            messageDispose.dispose();
        }
    }

    public void setAnswerJsCallback(PolyvAnswerWebView.AnswerJsCallback answerJsCallback) {
        if (answerWebView != null) {
            answerWebView.setAnswerJsCallback(answerJsCallback);
        }
    }

    public void destroy() {
        if (answerWebView != null) {
            answerWebView = null;
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onLandscape();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            onPortrait();
        }
    }

    //横屏回调
    private void onLandscape() {
        MarginLayoutParams lp = (MarginLayoutParams) answerWebView.getLayoutParams();
        lp.leftMargin = MarginConst.LANDSCAPE_MARGIN_LEFT;
        lp.rightMargin = lp.leftMargin;
        lp.topMargin = 0;
        lp.bottomMargin = MarginConst.LANDSCAPE_MARGIN_BOTTOM;
        answerWebView.setLayoutParams(lp);
    }

    //竖屏回调
    private void onPortrait() {
        MarginLayoutParams lp = (MarginLayoutParams) answerWebView.getLayoutParams();
        lp.leftMargin = MarginConst.PORTRAIT_MARGIN_LEFT;
        lp.rightMargin = lp.leftMargin;
        lp.topMargin = MarginConst.PORTRAIT_MARGIN_TOP;
        lp.bottomMargin = lp.topMargin;
        answerWebView.setLayoutParams(lp);
    }


    static class MarginConst {
        static final int LANDSCAPE_MARGIN_LEFT = toPx(80);
        static final int LANDSCAPE_MARGIN_BOTTOM = toPx(48);

        static final int PORTRAIT_MARGIN_LEFT = toPx(20);
        static final int PORTRAIT_MARGIN_TOP = toPx(60);

        private static int toPx(int dp) {
            return PolyvScreenUtils.dip2px(Utils.getApp(), dp);
        }
    }
}

