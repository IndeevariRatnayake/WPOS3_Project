package com.harshana.wposandroiposapp.Print;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.ParcelFileDescriptor;


import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class PDF
{
    private static final String IMAGE_FILE = "RcptImg.jpeg";
    private static PDF myInstance = null;
    private static final String PDF_DOC_NAME = "PdfDoc.pdf";
    private static String filePath ;
    private Context appContext;

    private static int IMG_WIDTH = 800;
    private static int IMG_HEIGHT = 800;


    public static PDF getInstance(Context context)
    {
        if (myInstance == null)
            myInstance = new PDF(context);

        return myInstance;
    }


    public static String importImageFilePath()
    {
        return Environment.getExternalStorageDirectory() + "/" + IMAGE_FILE;
    }

    Document doc = null;


    public static String importPDFFilePath()
    {
        return filePath;
    }
    //initializer
    public PDF(Context c)
    {
        if (doc == null)
            doc = new Document();

        appContext = c;

        filePath = Environment.getExternalStorageDirectory() + "/" + PDF_DOC_NAME;

        try {
            PdfWriter.getInstance(doc, new FileOutputStream(filePath));
            if (doc != null)
                doc.open();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //Rectangle rect = new Rectangle(IMG_WIDTH,IMG_HEIGHT);
        ///doc.setPageSize(rect);
    }


    private void addElement(Element ele)
    {
        try
        {
            if (!doc.isOpen())
                doc.open();

            doc.add(ele);
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }



    public void feedText(String text,float textSize,BaseColor color,boolean isBold)
    {
        int boldStatus = -1;

        textSize += 9;

        text  = Formatter.fillInFrontFixed(" ",text,10);

        if (isBold)
            boldStatus = Font.BOLD;
        else
            boldStatus = Font.NORMAL;

        Font font  = new Font(Font.FontFamily.COURIER,textSize,boldStatus, color);
        Paragraph para = new Paragraph(text,font);
        //para.setAlignment(Paragraph.ALIGN_CENTER);

        addElement(para);
    }




    public void feedTextCentered(String text,float textSize,BaseColor color,boolean isBold)
    {
        int boldStatus = -1;

        if (isBold)
            boldStatus = Font.BOLD;
        else
            boldStatus = Font.NORMAL;

        textSize += 9;

        Font font  = new Font(Font.FontFamily.COURIER,textSize,boldStatus, color);
        Paragraph para = new Paragraph(text,font);
        para.setAlignment(Paragraph.ALIGN_CENTER);
        addElement(para);
    }



    public void feedBlankLine()
    {
        Paragraph para = new Paragraph(" ");
        addElement(para);
    }


    public void feedImage(String imageName)
    {
        try
        {
            InputStream ins = appContext.getAssets().open(imageName);
            Bitmap bmp = BitmapFactory.decodeStream(ins);
            ByteArrayOutputStream byteArray =  new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG,100,byteArray);
            Image image = Image.getInstance(byteArray.toByteArray());
            image.setAlignment(Element.ALIGN_CENTER);
            addElement(image);

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }


    }


    public Bitmap generateBitmap()
    {
        PdfiumCore pdfcore = new PdfiumCore(appContext);

        Bitmap bitmap;
        try
        {
            File file =  new File(filePath);
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
            PdfDocument pdfDocument = pdfcore.newDocument(fd);
            pdfcore.openPage(pdfDocument,0);
            bitmap =  Bitmap.createBitmap(IMG_WIDTH,IMG_HEIGHT,Bitmap.Config.ARGB_8888) ;
            pdfcore.renderPageBitmap(pdfDocument,bitmap,0,0,0,IMG_WIDTH,IMG_HEIGHT);
            pdfcore.closeDocument(pdfDocument);

            file = new File(Environment.getExternalStorageDirectory() + "/",IMAGE_FILE);
            FileOutputStream fout = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fout);
            fout.flush();
            fout.close();
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return null;
        }

        return bitmap;
    }


    public void commmitFile()
    {
        doc.close();
        myInstance = null;
    }



}
