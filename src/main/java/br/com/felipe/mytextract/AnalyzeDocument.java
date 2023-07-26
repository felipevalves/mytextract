package br.com.felipe.mytextract;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Before running this Java V2 code example, set up your development environment, including your credentials.
 *
 * For more information, see the following documentation topic:
 *
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/get-started.html
 */
public class AnalyzeDocument {

    public static void main(String[] args) {

        final String usage = "\n" +
                "Usage:\n" +
                "    <sourceDoc> \n\n" +
                "Where:\n" +
                "    sourceDoc - The path where the document is located (must be an image, for example, C:/AWS/book.png). \n";

        if (args.length != 1) {
            System.out.println(usage);
            System.exit(1);
        }

        String sourceDoc = args[0];
        Region region = Region.US_EAST_2;
        TextractClient textractClient = TextractClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        //analyzeDoc(textractClient, sourceDoc);
        textractClient.close();
    }

    // snippet-start:[textract.java2._analyze_doc.main]
    public AnalyzeDocumentResponse analyzeDoc(TextractClient textractClient, String sourceDoc) {

        try {
            InputStream sourceStream = new FileInputStream(new File(sourceDoc));
            SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceStream);

            // Get the input Document object as bytes
            Document myDoc = Document.builder()
                    .bytes(sourceBytes)
                    .build();

            List<FeatureType> featureTypes = new ArrayList<FeatureType>();
            featureTypes.add(FeatureType.FORMS);
            featureTypes.add(FeatureType.TABLES);

            AnalyzeDocumentRequest analyzeDocumentRequest = AnalyzeDocumentRequest.builder()
                    .featureTypes(featureTypes)
                    .document(myDoc)
                    .build();

            AnalyzeDocumentResponse analyzeDocument = textractClient.analyzeDocument(analyzeDocumentRequest);

            System.out.println(analyzeDocument.toString());

            List<Block> docInfo = analyzeDocument.blocks();
            Iterator<Block> blockIterator = docInfo.iterator();

            while(blockIterator.hasNext()) {
                Block block = blockIterator.next();

//                if (block.blockType().equals(BlockType.KEY_VALUE_SET))
                   // System.out.println("Block type: " +block.blockType().toString() + " Text: " + block.text() + "  " + block.entityTypesAsStrings());


               // System.out.println("Block type: " +block.blockType().toString() + " Text: " + block.text() + " " + block.toString());
            }

            return analyzeDocument;
        } catch (TextractException | FileNotFoundException e) {

            System.err.println(e.getMessage());
        }
        return null;
    }

    public AnalyzeExpenseResponse analyzeExpense(TextractClient textractClient, String sourceDoc) {

        try {
            InputStream sourceStream = new FileInputStream(new File(sourceDoc));
            SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceStream);

            // Get the input Document object as bytes
            Document myDoc = Document.builder()
                    .bytes(sourceBytes)
                    .build();

            List<FeatureType> featureTypes = new ArrayList<FeatureType>();
            featureTypes.add(FeatureType.FORMS);
            featureTypes.add(FeatureType.TABLES);

            AnalyzeExpenseRequest request = AnalyzeExpenseRequest.builder()
                    .document(myDoc)
                    .build();

            AnalyzeExpenseResponse result = textractClient.analyzeExpense(request);

            return result;
        } catch (TextractException | FileNotFoundException e) {

            System.err.println(e.getMessage());
        }
        return null;
    }
}