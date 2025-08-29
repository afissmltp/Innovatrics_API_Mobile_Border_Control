package com.dynamsoft.documentscanner.model;

import android.content.Context;
import android.graphics.Bitmap;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class PdfGenerator {
    private Context context;

    // Constructeur qui accepte un Context
    public PdfGenerator(Context context) {
        this.context = context;
    }

    // Méthode qui n'accepte qu'un seul argument (les données)
    public void createPdf(Page1FragmentData page1Data) {
        File filesDir = context.getFilesDir();
        String fileName = "rapport_client.pdf";
        File pdfFile = new File(filesDir, fileName);
        String dest = pdfFile.getAbsolutePath();

        try {
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Rapport Client"));
            // ... Ajoutez les données du client
            document.add(new Paragraph("Date de naissance: " + page1Data.dob));

            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
