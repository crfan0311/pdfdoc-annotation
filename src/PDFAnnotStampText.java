import org.apache.pdfbox.pdmodel.PDAppearanceContentStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationRubberStamp;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.util.Matrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class PDFAnnotStampText {
    public static void main(String[] args) {

        //Creating PDF document object
        PDDocument document = null;
        String filename = "221113_DOS-POLLOS_Feuille-de-presence";
        //filename = "Document_de_test";
        try {
            document = PDDocument.load(new File("testfiles/" + filename + ".pdf"));
            document.getClass();

            if (!document.isEncrypted()) {
                System.out.println("not encrypted");

                // https://stackoverflow.com/questions/65927280/annotation-content-not-appearing-with-pdfbox
                OutputStream resultFile = new FileOutputStream("testfiles/" + filename + "_stamptxt.pdf");

                PDPage page = (PDPage) document.getPage(0);
                addAnnotation("annotation name", document, page, 50, 50, "Vikta powered");


                document.saveIncremental(resultFile);
                document.close();

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    private static void addAnnotation(String name, PDDocument doc, PDPage page, float x, float y, String text) throws IOException {

        List<PDAnnotation> annotations = page.getAnnotations();
        PDAnnotationRubberStamp t = new PDAnnotationRubberStamp();

        t.setAnnotationName(name); // might play important role
        t.setPrinted(true); // always visible
        t.setReadOnly(true); // does not interact with user
        t.setContents(text);

        // calculate realWidth, realHeight according to font size (e.g. using _font.getStringWidth(text))
        float realWidth = 100, realHeight = 100;
        PDRectangle rect = new PDRectangle(x, y, realWidth, realHeight);
        t.setRectangle(rect);

        PDAppearanceDictionary ap = new PDAppearanceDictionary();
        ap.setNormalAppearance(createAppearanceStream(doc, t));
        t.setAppearance(ap);

        annotations.add(t);
        page.setAnnotations(annotations);

        // these must be set for incremental save to work properly (PDFBOX < 3.0.0 at least?)
        ap.getCOSObject().setNeedToBeUpdated(true);
        t.getCOSObject().setNeedToBeUpdated(true);
        page.getResources().getCOSObject().setNeedToBeUpdated(true);
        page.getCOSObject().setNeedToBeUpdated(true);
        doc.getDocumentCatalog().getPages().getCOSObject().setNeedToBeUpdated(true);
        doc.getDocumentCatalog().getCOSObject().setNeedToBeUpdated(true);
    }

    private static void modifyAppearanceStream(PDAppearanceStream aps, PDAnnotation ann) throws IOException {
        PDAppearanceContentStream apsContent = null;

        try {
            PDRectangle rect = ann.getRectangle();
            rect = new PDRectangle(0, 0, rect.getWidth(), rect.getHeight()); // need to be relative - this is mega important because otherwise it appears as if nothing is printed
            aps.setBBox(rect); // set bounding box to the dimensions of the annotation itself

            // embed our unicode font (NB: yes, this needs to be done otherwise aps.getResources() == null which will cause NPE later during setFont)
            PDResources res = new PDResources();
            PDFont font = PDType1Font.HELVETICA_BOLD;
            res.add(font).getName(); // okay I create _font elsewhere
            aps.setResources(res);

            // draw directly on the XObject's content stream
            apsContent = new PDAppearanceContentStream(aps);

            apsContent.beginText();
            apsContent.setFont(PDType1Font.HELVETICA_BOLD, 12); // _font
            apsContent.setTextMatrix(Matrix.getTranslateInstance(0, 1));
            apsContent.showText(ann.getContents());
            apsContent.endText();
        }
        finally {
            if (apsContent != null) {
                try {
                    apsContent.close();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }

        aps.getResources().getCOSObject().setNeedToBeUpdated(true);
        aps.getCOSObject().setNeedToBeUpdated(true);
    }

    private static PDAppearanceStream createAppearanceStream(final PDDocument document, PDAnnotation ann) throws IOException
    {
        PDAppearanceStream aps = new PDAppearanceStream(document);
        modifyAppearanceStream(aps, ann);
        return aps;
    }
}

