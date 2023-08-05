package com.document.camerascanner.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import com.document.camerascanner.R;
import com.document.camerascanner.databases.model.DocumentItem;
import com.document.camerascanner.databases.model.PageItem;
import com.document.camerascanner.features.pdfsettings.SizeType;
import com.document.camerascanner.models.PDFOptions;
import com.document.camerascanner.prefs.AppPref;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PdfUtils {

    public static void convertPdf(Context context, int quality, String nameDocument, List<DocumentItem> documentItems, PdfStatus pdfStatus) {
        Single.create((SingleOnSubscribe<List<String>>) emitter -> {
            File saveDir = DbUtils.getSaveDir(context);
            PDFOptions pdfOptions = PdfUtils.getPdfOption(context);
            List<String> filePdf = new ArrayList<>();

            for (DocumentItem documentItem : documentItems) {
                Rectangle pageSize;
                if (pdfOptions.isPortrait()) {
                    pageSize = new Rectangle(pdfOptions.getPageSize());
                } else {
                    pageSize = new Rectangle(pdfOptions.getPageSize()).rotate();
                }

                pageSize.setBackgroundColor(getBaseColor());
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
                    String nameFilePdf = "";
                    if (!TextUtils.isEmpty(nameDocument)) {
                        nameFilePdf = nameDocument;
                    } else {
                        nameFilePdf = documentItem.getName();
                    }
                    nameFilePdf = context.getString(R.string.home_created_pdf_name, nameFilePdf) + Constants.PDF_EXTENSION;
                    nameFilePdf = FileUtils.createNameFilePdf(context, saveDir.getPath(), new File(saveDir, nameFilePdf));

                    File fileSave = new File(saveDir, nameFilePdf);
                    filePdf.add(fileSave.getPath());
                    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileSave));
                    document.open();

                    List<PageItem> listUris = documentItem.getListPage();
                    for (PageItem pageItem : listUris) {
                        Image image = Image.getInstance(compressImage(new File(pageItem.getEnhanceUri()), quality));
                        double qualityMod = (double) 30 * 0.09;
                        image.setCompressionLevel((int) qualityMod);
                        float pageWidth = document.getPageSize().getWidth() - (marginLeft + marginRight);
                        float pageHeight = document.getPageSize().getHeight() - (marginTop + marginBottom);
                        image.scaleToFit(pageWidth, pageHeight);
                        image.setAbsolutePosition((documentRect.getWidth() - image.getScaledWidth()) / 2,
                                (documentRect.getHeight() - image.getScaledHeight()) / 2);

                        if (pdfOptions.isPageNumber()) {
                            addPageNumber(documentRect, writer);
                        }

                        document.add(image);
                        document.newPage();
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
                .subscribe(new SingleObserver<List<String>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        if (pdfStatus != null) {
                            pdfStatus.onDisposable(d);
                        }
                    }

                    @Override
                    public void onSuccess(@NonNull List<String> strings) {
                        if (pdfStatus != null) {
                            pdfStatus.onSuccessConvertPdf(strings);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (pdfStatus != null) {
                            pdfStatus.onError();
                        }
                    }
                });

    }


    private static PDFOptions getPdfOption(Context context) {
        PDFOptions pdfOptions = new PDFOptions();
        AppPref appPref = AppPref.getInstance(context);
        pdfOptions.setPortrait(appPref.isPageOrientationPortrait());
        pdfOptions.setMargin(appPref.isMarginPdf());
        pdfOptions.setPageNumber(appPref.isPageNumber());
        int pageSize = appPref.getPageSizePdf();
        switch (pageSize) {
            case SizeType.LETTER:
                pdfOptions.setPageSize(PageSize.LETTER);
                break;
            case SizeType.A4:
                pdfOptions.setPageSize(PageSize.A4);
                break;
            case SizeType.LEGAL:
                pdfOptions.setPageSize(PageSize.LEGAL);
                break;
            case SizeType.A3:
                pdfOptions.setPageSize(PageSize.A3);
                break;
            case SizeType.A5:
                pdfOptions.setPageSize(PageSize.A5);
                break;
            case SizeType.BUSINESS_CARD:
                pdfOptions.setPageSize(PageSize.POSTCARD);
                break;
        }

        return pdfOptions;
    }

    private static int getAllItem(@androidx.annotation.NonNull List<DocumentItem> list) {
        int count = 0;
        for (DocumentItem itemFile : list) {
            count += itemFile.getListPage().size();
        }
        return count;
    }

    @Contract(" -> new")
    private static @NotNull BaseColor getBaseColor() {
        return new BaseColor(Color.red(Color.WHITE), Color.green(Color.WHITE), Color.blue(Color.WHITE));
    }

    private static void addPageNumber(@androidx.annotation.NonNull Rectangle documentRect, @androidx.annotation.NonNull PdfWriter writer) {
        ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_BOTTOM | Element.ALIGN_RIGHT,
                getPhrase(writer), ((documentRect.getRight()) - 25), documentRect.getBottom() + 25, 0);
    }

    @androidx.annotation.NonNull
    @SuppressLint("DefaultLocale")
    private static Phrase getPhrase(@androidx.annotation.NonNull PdfWriter writer) {
        Phrase phrase;
        phrase = new Phrase(String.format("%d", writer.getPageNumber()));
        return phrase;
    }

    public static byte[] compressImage(@NotNull File fileImage, int quality) {
        Bitmap bitmap = BitmapFactory.decodeFile(fileImage.getPath());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        return out.toByteArray();
    }


    public interface PdfStatus {

        void onDisposable(Disposable disposable);

        void onSuccessConvertPdf(List<String> listPdf);

        void onProcessPercent(int percent);

        void onError();
    }
}
