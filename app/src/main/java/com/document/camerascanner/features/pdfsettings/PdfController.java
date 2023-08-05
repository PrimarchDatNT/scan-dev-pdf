package com.document.camerascanner.features.pdfsettings;

import android.content.Context;
import android.content.Intent;

import com.itextpdf.text.Rectangle;

import java.util.List;

public interface PdfController {

    interface PdfPresenter {

        void init(Context context);

        void getListImage(Intent intent);

        void setPageOrientation();

        void setPageNumber(Context context, boolean isPageNumber);

        void setPageMargin(boolean isPageMargin);

        void setPageSize(Rectangle pageSize);

        void convertImageToPdf(Context context);

        void disposableAll();
    }

    interface PdfView {

        void showPageOrientation(int orientation);

        void showPageSizeInit(int pageSize);

        void showContentPageNumber(String content);

        void onSuccessGetListImage();

        void onSuccessConvertPdf(List<String> filesPdf);

        void showNoMarginPage();

        void showAddMarginPage();

        void showPercent(int percent);
    }
}
