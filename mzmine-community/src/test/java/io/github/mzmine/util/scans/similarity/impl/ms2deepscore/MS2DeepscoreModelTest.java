package io.github.mzmine.util.scans.similarity.impl.ms2deepscore;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import io.github.mzmine.datamodel.PolarityType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MS2DeepscoreModelTest {
    private static MS2DeepscoreModel model;

    @BeforeAll
    static void setUp() throws URISyntaxException, ModelNotFoundException, MalformedModelException, IOException {
        // load model and setup objects that are shared with all tests
        MassSpecTestData spec = new MassSpecTestData(
                new double[]{200d, 200.1d}, new double[]{1000d, 2000d},
                PolarityType.POSITIVE, 221d);
        URI modelFilePath = MS2DeepscoreModelTest.class.getClassLoader()
                .getResource("models/java_compatible_ms2deepscore_model.pt").toURI();
        model = new MS2DeepscoreModel(modelFilePath);

    }


    @AfterAll
    static void tearDown() {
    }

    // Method to generate a list of 1000 random values
    private float[][] generateRandomList(int listLength, int numberOfArrays) {
        float[][] listOfList = new float[numberOfArrays][listLength];
        for (int i = 0; i < numberOfArrays; i++) {
            Random random = new Random();
            for (int j = 0; j < listLength; j++) {
                float randomNumber = random.nextFloat();
                listOfList[i][j] = randomNumber;
            }
        }

        return listOfList;
    }

    private float[][] generateList(int listLength, float listValues) {
        float[][] listOfList = new float[1][listLength];
        for (int i = 0; i < listLength; i++) {
            listOfList[0][i] = listValues;
        }
        return listOfList;
    }
    @Test
    void test_correct_prediction(){
        try (NDManager manager = NDManager.newBaseManager()) {
//                int nr_of_spectra = 3;
            float[][] spectrumArray1 = generateList(990, 0.1F);
            float[][] spectrumArray2 = generateList(990, 0.2F);
            float[][] metadataArray1 = generateList(2, 0.0F);
            float[][] metadataArray2 = generateList(2, 1.0F);
            // Convert input float array to NDArray

            NDArray spectrumNDArray1 = manager.create(spectrumArray1);
            NDArray spectrumNDArray2 = manager.create(spectrumArray2);
            NDArray metadataNDArray1 = manager.create(metadataArray1);
            NDArray metadataNDArray2 = manager.create(metadataArray2);


//                System.out.println(spectrumNDArray1);
            NDList predictions = model.predict(spectrumNDArray1, spectrumNDArray2, metadataNDArray1, metadataNDArray2);
            assertEquals(predictions.size(), 1);
            assertEquals(predictions.getFirst().size(), 1);
            assertEquals(predictions.get(0).getFloat(0), 0.9916, 0.0001);

            System.out.println(Arrays.toString(predictions.toArray()));

        } catch (TranslateException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test_multiple_predictions() throws ModelNotFoundException, MalformedModelException, IOException {
        try (NDManager manager = NDManager.newBaseManager()) {
            int nr_of_spectra = 3;
            NDArray spectrumNDArray1 = manager.create(generateRandomList(990, nr_of_spectra));
            NDArray spectrumNDArray2 = manager.create(generateRandomList(990, nr_of_spectra));
            NDArray metadataNDArray1 = manager.create(generateRandomList(2, nr_of_spectra));
            NDArray metadataNDArray2 = manager.create(generateRandomList(2, nr_of_spectra));


            NDList predictions = model.predict(spectrumNDArray1, spectrumNDArray2, metadataNDArray1, metadataNDArray2);
            assertEquals(predictions.size(), 1);
            assertEquals(predictions.getFirst().size(), nr_of_spectra);

        } catch (TranslateException e) {
            throw new RuntimeException(e);
        }
    }
}

