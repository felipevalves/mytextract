package br.com.felipe.mytextract;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@SpringBootTest
class MytextractApplicationTests {

	@Value("${aws.region.norte-virginia}")
	private String regionNorteVirginia;
	private TextractClient client;

	/**
	 * DEFAULT - MILI-DESENVOLVIMENTO
	 *
	 */
	@Bean("S3ClientDefaultNorteVirginia")
	public TextractClient getAuthenticationNorteVirginia() {
		return TextractClient.builder().region(Region.of(regionNorteVirginia)).credentialsProvider(ProfileCredentialsProvider.create("default")).build();
	}

	@Test
	void contextLoads() {
	}

	@BeforeEach
	void setup() {
		Region region = Region.US_EAST_2;
		client = TextractClient.builder().region(Region.of(regionNorteVirginia)).credentialsProvider(ProfileCredentialsProvider.create("default")).build();
	}

	@AfterEach
	void close() {
		client.close();
	}

	@Test
	void test_analyze() {
		AnalyzeDocument a = new AnalyzeDocument();
		a.analyzeDoc(client,"C:\\doc\\aws\\cupom-fiscal\\mili-01.jpg");
	}

	@Test
	void test_detect() {
		DetectDocument a = new DetectDocument();
		a.detectDocText(client,"C:\\doc\\aws\\cupom-fiscal\\mili-01.jpg");

	}

	@Test
	void test_analyze_expense() {
		AnalyzeDocument a = new AnalyzeDocument();
		AnalyzeExpenseResponse response = a.analyzeExpense(client, "pic\\mili-02.jpg");

		System.out.println(response.documentMetadata().toString());

		for (ExpenseDocument d: response.expenseDocuments()) {

			System.out.println("Document: " + d.expenseIndex());

			for (ExpenseField field : d.summaryFields()) {
				if (field.labelDetection() == null)
					System.out.println(field.type().text() + ": " + field.valueDetection().text());
				else
					System.out.println(field.labelDetection().text() + ": " + field.valueDetection().text());
			}
			System.out.println("===");
			for (LineItemGroup g : d.lineItemGroups()) {
				for (LineItemFields f : g.lineItems()) {
					for (ExpenseField ef : f.lineItemExpenseFields()) {
						if (ef.labelDetection() == null)
							System.out.println(ef.type().text() + ": " + ef.valueDetection().text());
						else
							System.out.println(ef.labelDetection().text() + ": " + ef.valueDetection().text());
					}

				}
			}

		}

	}

	@Test
	void test_analyze_key_value() {
		AnalyzeDocument a = new AnalyzeDocument();
		LocalDateTime ini = LocalDateTime.now();
		AnalyzeDocumentResponse response = a.analyzeDoc(client, "pic\\mercado-despesa-0507202312.jpeg");

		List<Block> blocks = response.blocks();

		// Get key and value maps
		Map<String, Block> keyMap = new HashMap<>();
		Map<String, Block> valueMap = new HashMap<>();
		Map<String, Block> cellMap = new HashMap<>();
		Map<String, Block> blockMap = getBlockMap(blocks);

		for (Block block : blocks) {
			if (BlockType.KEY_VALUE_SET.equals(block.blockType())) {
				if (block.entityTypes().contains(EntityType.KEY))
					keyMap.put(block.id(), block);
				else
					valueMap.put(block.id(), block);
			}
			else if (BlockType.CELL.equals(block.blockType())) {
				if (block.entityTypes().isEmpty())
					cellMap.put(block.id(), block);
			}
		}

		// Get Key Value relationship
		Map<String, String> kvs = getKeyValueRelationship(keyMap, valueMap, blockMap);

		List<String> cells = getKeyValueCellRelationship(cellMap, blockMap);

		System.out.println("\n\n== FOUND KEY : VALUE pairs ===\n");
		printKvs(kvs);

		System.out.println("Table: " + cells);

		System.out.println(searchValue(kvs, "cnpj"));

		LocalDateTime fim = LocalDateTime.now();

		Duration dur = Duration.between(ini, fim);

		System.out.println("Tempo: " + dur.getSeconds());
	}


	public static Map<String, Block> getBlockMap(List<Block> blocks) {
		Map<String, Block> blockMap = new HashMap<>();
		for (Block block : blocks) {
			blockMap.put(block.id(), block);
		}
		return blockMap;
	}

	public static String getText(Block result, Map<String, Block> blockMap) {
		StringBuilder text = new StringBuilder();
		if (result.relationships() != null) {
			for (Relationship relationship : result.relationships()) {

				if (RelationshipType.CHILD.equals(relationship.type())) {
					for (String childId : relationship.ids()) {
						Block word = blockMap.get(childId);
						if (word != null && BlockType.WORD.equals(word.blockType())) {
							text.append(word.text()).append(' ');
						}
						if (word != null && BlockType.SELECTION_ELEMENT.equals(word.blockType())) {
							if (word.selectionStatus() != null && word.selectionStatus().toString().equals("SELECTED")) {
								text.append("X ");
							}
						}
					}
				}
			}
		}
		return text.toString();
	}

	public static Block findValueBlock(Block keyBlock, Map<String, Block> valueMap) {
		if (keyBlock.relationships() != null) {
			for (Relationship relationship : keyBlock.relationships()) {
				if (RelationshipType.VALUE.equals(relationship.type())) {
					for (String valueId : relationship.ids()) {
						return valueMap.get(valueId);
					}
				}
			}
		}
		return null;
	}

	public static Map<String, String> getKeyValueRelationship(Map<String, Block> keyMap, Map<String, Block> valueMap,
															  Map<String, Block> blockMap) {
		Map<String, String> kvs = new HashMap<>();
		for (Block keyBlock : keyMap.values()) {
			Block valueBlock = findValueBlock(keyBlock, valueMap);
			String key = getText(keyBlock, blockMap);
			String val = getText(valueBlock, blockMap);
			kvs.put(key, val);
		}
		return kvs;
	}

	public static List<String> getKeyValueCellRelationship(Map<String, Block> cellMap, Map<String, Block> blockMap) {
		List<String> cells = new ArrayList<>();
		for (Block keyBlock : cellMap.values()) {
			String val = getText(keyBlock, blockMap);
			cells.add(val);
		}
		return cells;
	}

	public static void printKvs(Map<String, String> kvs) {
		for (Map.Entry<String, String> entry : kvs.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
	}

	public static String searchValue(Map<String, String> kvs, String searchKey) {
		for (Map.Entry<String, String> entry : kvs.entrySet()) {
			String key = entry.getKey();
			if (Pattern.compile(Pattern.quote(searchKey), Pattern.CASE_INSENSITIVE).matcher(key).find()) {
				return entry.getValue();
			}
		}
		return null;
	}

}
