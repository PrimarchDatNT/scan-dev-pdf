package com.document.camerascanner.features.pdfsettings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.Toast;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.AppDatabases;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.models.PDFOptions;
import com.document.camerascanner.prefs.AppPref;
import com.document.camerascanner.utils.Constants;
import com.document.camerascanner.utils.DbUtils;
import com.document.camerascanner.utils.FileUtils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfWriter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PdfPresenter implements PdfController.PdfPresenter {

    private static final int QUALITY_IMAGE = 30;
    private int typeSize = 0;

    private final PDFOptions pdfOptions;
    private final PdfController.PdfView pdfView;

    private AppDatabases appDatabases;
    private CompositeDisposable compositeDisposable;
    private List<DocumentItem> listImage = new ArrayList<>();

    public PdfPresenter(PdfController.PdfView pdfView) {
        this.pdfView = pdfView;
        this.pdfOptions = new PDFOptions();
    }

    @Override
    public void init(Context context) {
        AppPref appPref = AppPref.getInstance(context);
        this.pdfOptions.setPortrait(appPref.isPageOrientationPortrait());
        this.pdfOptions.setPageNumber(appPref.isPageNumber());
        this.appDatabases = AppDatabases.getInstance(context);

        int pageSize = appPref.getPageSizePdf();
        switch (pageSize) {
            case SizeType.LETTER:
                this.pdfOptions.setPageSize(PageSize.LETTER);
                this.typeSize = SizeType.LETTER;
                this.callBackPageSizeInit(R.string.pdf_settings_size_letter);
                break;
            case SizeType.A4:
                this.pdfOptions.setPageSize(PageSize.A4);
                this.typeSize = SizeType.A4;
                this.callBackPageSizeInit(R.string.pdf_settings_size_a4);
                break;
            case SizeType.LEGAL:
                this.pdfOptions.setPageSize(PageSize.LEGAL);
                this.typeSize = SizeType.LEGAL;
                this.callBackPageSizeInit(R.string.pdf_settings_size_legal);
                break;
            case SizeType.A3:
                this.pdfOptions.setPageSize(PageSize.A3);
                this.typeSize = SizeType.A3;
                this.callBackPageSizeInit(R.string.pdf_settings_size_a3);
                break;
            case SizeType.A5:
                this.pdfOptions.setPageSize(PageSize.A5);
                this.typeSize = SizeType.A5;
                this.callBackPageSizeInit(R.string.pdf_settings_size_a5);
                break;
            case SizeType.BUSINESS_CARD:
                this.pdfOptions.setPageSize(PageSize.POSTCARD);
                this.typeSize = SizeType.BUSINESS_CARD;
                this.callBackPageSizeInit(R.string.pdf_settings_size_bussiness_card);
                break;
        }
    }

    @Override
    public void getListImage(Intent intent) {
        Completable.create(emitter -> {
            if (intent == null) {
                emitter.onError(new NullPointerException());
                return;
            }

            List<Integer> listItemSelect = intent.getIntegerArrayListExtra(Constants.EXTRA_SHARE_DOCUMENT);
            if (listItemSelect != null) {
                this.listImage = this.appDatabases.documentDao().getDocumentsByIds(listItemSelect);

                for (DocumentItem documentItem : this.listImage) {
                    documentItem.setListPage(this.appDatabases.pageDao().getListPageByDocumentIdNoRx(documentItem.getId()));
                }

                emitter.onComplete();
                return;
            }

            ArrayList<Integer> listImageSelect;
            listImageSelect = intent.getIntegerArrayListExtra(Constants.EXTRA_IMAGE_SELECT);
            if (listImageSelect == null || listImageSelect.size() == 0) {
                emitter.onError(new Throwable());
                return;
            }

            List<PageItem> listPageItemSelect;
            List<PageItem> listPageSort = new ArrayList<>();
            listPageItemSelect = this.appDatabases.pageDao().getListPageByListIdNoRx(listImageSelect);

            for (int i = 0; i < listImageSelect.size(); i++) {
                int id = listImageSelect.get(i);

                for (int j = 0; j < listPageItemSelect.size(); j++) {
                    PageItem pageItem = listPageItemSelect.get(j);

                    if (id == pageItem.getId()) {
                        listPageSort.add(pageItem);
                        listPageItemSelect.remove(j);
                        break;
                    }
                }
            }

            DocumentItem documentItem = this.appDatabases.documentDao().getDocumentByIdNoRx(listPageSort.get(0).getParentId());
            documentItem.setListPage(listPageSort);
            this.listImage.add(documentItem);

            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onComplete() {
                        callBackSuccessGetListImage();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void setPageNumber(@NotNull Context context, boolean isPageNumber) {
        this.pdfOptions.setPageNumber(isPageNumber);
        this.callBackPageNumber(context.getString(isPageNumber ? R.string.pdf_settings_show_page_number : R.string.pdf_settings_hide_page_number));
    }

    @Override
    public void setPageMargin(boolean isPageMargin) {
        this.pdfOptions.setMargin(isPageMargin);

        if (isPageMargin) {
            this.callBackAddMarginPage();
        } else {
            this.callBackNoMarginPage();
        }
    }

    @Override
    public void setPageOrientation() {
        this.pdfOptions.setPortrait(!this.pdfOptions.isPortrait());
        int pageOrientation = this.pdfOptions.isPortrait() ? R.string.pdf_settings_portrait : R.string.pdf_settings_landscape;
        this.callBackPageOrientation(pageOrientation);
    }

    @Override
    public void setPageSize(Rectangle pageSize) {
        this.pdfOptions.setPageSize(pageSize);
    }

    @Override
    public void convertImageToPdf(Context context) {
        String orientation = this.pdfOptions.isPortrait() ? "PORTRAIT" : "LANDSCAPE";
        String margin = this.pdfOptions.isMargin() ? "ON" : "OFF";
        String number = this.pdfOptions.isPageNumber() ? "ON" : "OFF";
        FirebaseAnalytics.getInstance(context).logEvent("PDF_ORIENTATION_" + orientation, null);
        FirebaseAnalytics.getInstance(context).logEvent("PDF_MARGIN_" + margin, null);
        FirebaseAnalytics.getInstance(context).logEvent("PDF_NUMBER_" + number, null);
        AppPref appPref = AppPref.getInstance(context);
        appPref.setPageOrientationPortrait(this.pdfOptions.isPortrait());
        appPref.setPageSizePdf(this.typeSize);
        appPref.setPageNumber(this.pdfOptions.isPageNumber());
        appPref.setMarginPdf(this.pdfOptions.isMargin());
//        this.convertImageToPdf(context, this.listImage, this.pdfOptions);
        ((Activity) context).finish();
    }

    @androidx.annotation.NonNull
    @SuppressLint("DefaultLocale")
    private Phrase getPhrase(@androidx.annotation.NonNull PdfWriter writer) {
        Phrase phrase;
        phrase = new Phrase(String.format("%d", writer.getPageNumber()));
        return phrase;
    }

    private int getAllItem(@androidx.annotation.NonNull List<DocumentItem> list) {
        int count = 0;
        for (DocumentItem itemFile : list) {
            count += itemFile.getListPage().size();
        }
        return count;
    }

    @Contract(" -> new")
    private @NotNull BaseColor getBaseColor() {
        return new BaseColor(Color.red(Color.WHITE), Color.green(Color.WHITE), Color.blue(Color.WHITE));
    }

    private void convertImageToPdf(Context context, List<DocumentItem> list, PDFOptions pdfOptions) {
        File saveDir = DbUtils.getSaveDir(context);

        Maybe.create((MaybeOnSubscribe<List<String>>) emitter -> {
            int countAll = this.getAllItem(list);
            int countCurrent = 0;
            List<String> filePdf = new ArrayList<>();

            for (DocumentItem documentItem : list) {
                Rectangle pageSize;
                if (pdfOptions.isPortrait()) {
                    pageSize = new Rectangle(pdfOptions.getPageSize());
                } else {
                    pageSize = new Rectangle(pdfOptions.getPageSize()).rotate();
                }

                pageSize.setBackgroundColor(this.getBaseColor());
                int marginLeft = 0;
                int marginRight = 0;
                int marginTop = 0;
                int marginBottom = 0;

                if (pdfOptions.isMargin()) {
                    marginLeft = 30;
                    marginTop = 30;
                    marginRight = 20;
                    marginBottom = 25;
                }

                Document document = new Document(pageSize, marginLeft, marginRight, marginTop, marginBottom);
                document.setMargins(marginLeft, marginRight, marginTop, marginBottom);
                Rectangle documentRect = document.getPageSize();

                try {
                    String nameFilePdf = documentItem.getName();
                    nameFilePdf = context.getString(R.string.home_created_pdf_name, nameFilePdf) + Constants.PDF_EXTENSION;
                    nameFilePdf = FileUtils.createNameFilePdf(context, saveDir.getPath(), new File(saveDir, nameFilePdf));

                    File fileSave = new File(saveDir, nameFilePdf);
                    filePdf.add(fileSave.getPath());
                    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileSave));
                    document.open();

                    List<PageItem> listUris = documentItem.getListPage();
                    for (PageItem pageItem : listUris) {
                        Image image = Image.getInstance(pageItem.getEnhanceUri());
                        double qualityMod = (double) QUALITY_IMAGE * 0.09;
                        image.setCompressionLevel((int) qualityMod);
                        float pageWidth = document.getPageSize().getWidth() - (marginLeft + marginRight);
                        float pageHeight = document.getPageSize().getHeight() - (marginTop + marginBottom);
                        image.scaleToFit(pageWidth, pageHeight);
                        image.setAbsolutePosition((documentRect.getWidth() - image.getScaledWidth()) / 2,
                                (documentRect.getHeight() - image.getScaledHeight()) / 2);

                        if (pdfOptions.isPageNumber()) {
                            this.addPageNumber(documentRect, writer);
                        }

                        document.add(image);
                        document.newPage();
                        countCurrent++;
                        int percent = (countCurrent * 100) / countAll;

                        try {
                            this.callBackPercent(percent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    document.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    emitter.onError(e);
                }
            }
            emitter.onSuccess(filePdf);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new MaybeObserver<List<String>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(@NonNull List<String> listResult) {
                        Toast.makeText(context, context.getString(R.string.pdf_settings_save_dir, saveDir.getPath()), Toast.LENGTH_SHORT).show();
                        callBackSuccessConvertPdf(listResult);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    public void setTypeSize(int typeSize) {
        this.typeSize = typeSize;
    }

    @Override
    public void disposableAll() {
        if (this.compositeDisposable != null) {
            this.compositeDisposable.dispose();
        }
    }

    private void addPageNumber(@androidx.annotation.NonNull Rectangle documentRect, @androidx.annotation.NonNull PdfWriter writer) {
        ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_BOTTOM | Element.ALIGN_RIGHT,
                this.getPhrase(writer), ((documentRect.getRight()) - 25), documentRect.getBottom() + 25, 0);
    }

    private void callBackPageOrientation(int orientation) {
        if (this.pdfView == null) {
            return;
        }
        this.pdfView.showPageOrientation(orientation);
    }

    private void callBackPageNumber(String content) {
        if (this.pdfView == null) {
            return;
        }
        this.pdfView.showContentPageNumber(content);
    }

    private void callBackSuccessGetListImage() {
        if (this.pdfView == null) {
            return;
        }
        this.pdfView.onSuccessGetListImage();
    }

    private void callBackSuccessConvertPdf(List<String> listFilePdf) {
        if (this.pdfView == null) {
            return;
        }
        this.pdfView.onSuccessConvertPdf(listFilePdf);
    }

    private void callBackAddMarginPage() {
        if (this.pdfView == null) {
            return;
        }
        this.pdfView.showAddMarginPage();
    }

    private void callBackNoMarginPage() {
        if (this.pdfView == null) {
            return;
        }
        this.pdfView.showNoMarginPage();
    }

    private void callBackPercent(int percent) {
        if (this.pdfView == null) {
            return;
        }
        this.pdfView.showPercent(percent);
    }

    private void callBackPageSizeInit(int pageSize) {
        if (this.pdfView == null) {
            return;
        }
        this.pdfView.showPageSizeInit(pageSize);
    }

    private void addDisposable(Disposable disposable) {
        if (this.compositeDisposable == null) {
            this.compositeDisposable = new CompositeDisposable();
        }

        if (disposable == null) {
            return;
        }

        this.compositeDisposable.add(disposable);
    }


}
