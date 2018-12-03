package edu.uoc.som.jarvis.core.recognition.dialogflow;

import com.google.cloud.dialogflow.v2.Intent;
import edu.uoc.som.jarvis.AbstractJarvisTest;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.core.session.RuntimeContexts;
import edu.uoc.som.jarvis.intent.*;
import edu.uoc.som.jarvis.test.util.VariableLoaderHelper;
import edu.uoc.som.jarvis.test.util.models.TestExecutionModel;
import fr.inria.atlanmod.commons.log.Log;
import edu.uoc.som.jarvis.AbstractJarvisTest;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.session.RuntimeContexts;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.intent.*;
import edu.uoc.som.jarvis.test.util.VariableLoaderHelper;
import edu.uoc.som.jarvis.test.util.models.TestExecutionModel;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DialogFlowApiTest extends AbstractJarvisTest {

    protected static String VALID_PROJECT_ID = VariableLoaderHelper.getJarvisDialogFlowProject();

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static String SAMPLE_INPUT = "hello";

    protected static EntityDefinition VALID_ENTITY_DEFINITION;

    /*
     * EntityDefinition is contained in the context parameter, we need to create a second testing instance.
     */
    protected static EntityDefinition VALID_ENTITY_DEFINITION_2;

    protected static IntentDefinition VALID_INTENT_DEFINITION_WITH_OUT_CONTEXT;

    protected static Context VALID_OUT_CONTEXT;

    protected static String VALID_TRAINING_SENTENCE_WITHOUT_CONTEXT_PARAMETER = "I love the monkey head";

    protected static String VALID_TRAINING_SENTENCE_WITH_CONTEXT_PARAMETER = "I love the test monkey head";

    protected DialogFlowApi api;

    /**
     * Stores the last {@link IntentDefinition} registered by
     * {@link DialogFlowApi#registerIntentDefinition(IntentDefinition)}.
     * <p>
     * <b>Note:</b> this variable must be set by each test case calling
     * {@link DialogFlowApi#registerIntentDefinition(IntentDefinition)}, to enable their deletion in the
     * {@link #tearDown()} method. Not setting this variable would add test-related intents in the DialogFlow project.
     *
     * @see #tearDown()
     */
    private IntentDefinition registeredIntentDefinition;

    // not tested here, only instantiated to enable IntentDefinition registration and Platform retrieval
    protected static JarvisCore jarvisCore;

    private static Configuration buildConfiguration(String projectId, String languageCode) {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowApi.PROJECT_ID_KEY, projectId);
        configuration.addProperty(DialogFlowApi.LANGUAGE_CODE_KEY, languageCode);
        /*
         * Disable Intent loading to avoid RESOURCE_EXHAUSTED exceptions from the DialogFlow API.
         */
        configuration.addProperty(DialogFlowApi.ENABLE_INTENT_LOADING_KEY, false);
        return configuration;
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        TestExecutionModel testExecutionModel = new TestExecutionModel();
        Configuration configuration = buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        configuration.addProperty(JarvisCore.EXECUTION_MODEL_KEY, testExecutionModel.getExecutionModel());
        jarvisCore = new JarvisCore(configuration);
        VALID_ENTITY_DEFINITION = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        ((BaseEntityDefinition) VALID_ENTITY_DEFINITION).setEntityType(EntityType.ANY);
        VALID_ENTITY_DEFINITION_2 = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        ((BaseEntityDefinition) VALID_ENTITY_DEFINITION_2).setEntityType(EntityType.ANY);
        VALID_OUT_CONTEXT = IntentFactory.eINSTANCE.createContext();
        VALID_OUT_CONTEXT.setName("ValidContext");
        ContextParameter contextParameter = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter.setName("param");
        contextParameter.setTextFragment("test");
        BaseEntityDefinition entityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        entityDefinition.setEntityType(EntityType.ANY);
        contextParameter.setEntity(entityDefinition);
        VALID_OUT_CONTEXT.getParameters().add(contextParameter);
        VALID_INTENT_DEFINITION_WITH_OUT_CONTEXT = IntentFactory.eINSTANCE.createIntentDefinition();
        VALID_INTENT_DEFINITION_WITH_OUT_CONTEXT.setName("TestIntentDefinition");
        VALID_INTENT_DEFINITION_WITH_OUT_CONTEXT.getTrainingSentences().add("test intent definition");
        VALID_INTENT_DEFINITION_WITH_OUT_CONTEXT.getOutContexts().add(VALID_OUT_CONTEXT);
    }

    @After
    public void tearDown() {
        if (nonNull(registeredIntentDefinition)) {
            api.deleteIntentDefinition(registeredIntentDefinition);
            jarvisCore.getEventDefinitionRegistry().unregisterEventDefinition(registeredIntentDefinition);
        }
        /*
         * Reset the variable value to null to avoid unnecessary deletion calls.
         */
        registeredIntentDefinition = null;
        if (nonNull(api)) {
            try {
                api.shutdown();
            } catch (DialogFlowException e) {
                /*
                 * Already shutdown, ignore
                 */
            }
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        jarvisCore.shutdown();
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private DialogFlowApi getValidDialogFlowApi() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE));
        return api;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullJarvisCore() {
        api = new DialogFlowApi(null, buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE));
    }

    @Test(expected = NullPointerException.class)
    public void constructNullProjectIdValidLanguageCode() {
        api = new DialogFlowApi(null, buildConfiguration(null, "en-US"));
    }

    @Test
    public void constructNullLanguageCode() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration(VALID_PROJECT_ID, null));
        assertThat(api.getLanguageCode()).as("Default language code").isEqualTo(DialogFlowApi.DEFAULT_LANGUAGE_CODE);
    }

    @Test
    public void constructValid() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE));
        softly.assertThat(VALID_PROJECT_ID).as("Valid project ID").isEqualTo(api.getProjectId());
        softly.assertThat(VALID_LANGUAGE_CODE).as("Valid language code").isEqualTo(api.getLanguageCode());
        softly.assertThat(api.isShutdown()).as("Not shutdown").isFalse();
    }

    @Test
    public void constructDefaultLanguageCode() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration(VALID_PROJECT_ID, null));
        softly.assertThat(VALID_PROJECT_ID).as("Valid project ID").isEqualTo(api.getProjectId());
        softly.assertThat(VALID_LANGUAGE_CODE).as("Valid language code").isEqualTo(api.getLanguageCode());
    }

    @Test
    public void constructCredentialsFilePath() {
        Configuration configuration = buildConfiguration(VALID_PROJECT_ID, null);
        String credentialsFilePath = this.getClass().getClassLoader().getResource("jarvis-secret.json").getFile();
        configuration.addProperty(DialogFlowApi.GOOGLE_CREDENTIALS_PATH_KEY, credentialsFilePath);
        api = new DialogFlowApi(jarvisCore, configuration);
        /*
         * Ensures that the underlying IntentsClient credentials are valid by listing the Agent intents.
         */
        Log.info("Listing DialogFlow intents to check permissions");
        for (Intent registeredIntent : api.getRegisteredIntents()) {
            Log.info("Found intent {0}", registeredIntent.getDisplayName());
        }
        /*
         * Ensures that the underlying AgentsClient credentials are valid by training the Agent.
         */
        api.trainMLEngine();
    }

    @Test(expected = NullPointerException.class)
    public void registerIntentDefinitionNullIntentDefinition() {
        api = getValidDialogFlowApi();
        api.registerIntentDefinition(null);
    }

    @Test(expected = NullPointerException.class)
    public void registerIntentDefinitionNullName() {
        api = getValidDialogFlowApi();
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        api.registerIntentDefinition(intentDefinition);
    }

    @Test
    public void registerIntentDefinitionValidIntentDefinition() {
        api = getValidDialogFlowApi();
        registeredIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        String intentName = "TestRegister";
        String trainingPhrase = "test";
        registeredIntentDefinition.setName(intentName);
        registeredIntentDefinition.getTrainingSentences().add(trainingPhrase);
        api.registerIntentDefinition(registeredIntentDefinition);
        List<Intent> registeredIntents = api.getRegisteredIntentsFullView();
        assertThat(registeredIntents).as("Registered Intent list is not null").isNotNull();
        softly.assertThat(registeredIntents).as("Registered Intent list is not empty").isNotEmpty();
        Intent foundIntent = null;
        for (Intent intent : registeredIntents) {
            if (intent.getDisplayName().equals(intentName)) {
                foundIntent = intent;
            }
        }
        assertThat(foundIntent).as("Registered Intent list contains the registered IntentDefinition")
                .isNotNull();
        softly.assertThat(foundIntent.getTrainingPhrasesList()).as("Intent's training phrase list is not empty")
                .isNotEmpty();
        boolean foundTrainingPhrase = hasTrainingPhrase(foundIntent, trainingPhrase);
        softly.assertThat(foundTrainingPhrase).as("The IntentDefinition's training phrase is in the retrieved " +
                "Intent").isTrue();
    }

    @Test
    public void registerIntentDefinitionFollowUpIntentDefinition() {
        api = getValidDialogFlowApi();
        /*
         * Put the parent as the registered one, so all the follow-up intents will be deleted after the test case.
         */
        registeredIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        String parentIntentName = "TestRegisterParent";
        String parentTrainingSentence = "test parent";
        registeredIntentDefinition.setName(parentIntentName);
        registeredIntentDefinition.getTrainingSentences().add(parentTrainingSentence);
        IntentDefinition followUpIntent = IntentFactory.eINSTANCE.createIntentDefinition();
        String followUpIntentName = "TestRegisterFollowUp";
        String followUpTrainingSentence = "test followUp";
        followUpIntent.setName(followUpIntentName);
        followUpIntent.getTrainingSentences().add(followUpTrainingSentence);
        followUpIntent.setFollows(registeredIntentDefinition);
        api.registerIntentDefinition(followUpIntent);
        List<Intent> registeredIntents = api.getRegisteredIntentsFullView();
        assertThat(registeredIntents).as("Registered Intent list is not null").isNotNull();
        softly.assertThat(registeredIntents).as("Registered Intent list is not empty").isNotEmpty();
        Intent foundParentIntent = null;
        Intent foundFollowUpIntent = null;
        for(Intent intent : registeredIntents) {
            if(intent.getDisplayName().equals(parentIntentName)) {
                foundParentIntent = intent;
            } else if(intent.getDisplayName().equals(followUpIntentName)) {
                foundFollowUpIntent = intent;
            }
        }
        assertThat(foundParentIntent).as("Registered Intent list contains the parent IntentDefinition").isNotNull();
        assertThat(foundParentIntent.getTrainingPhrasesList()).as("Parent Intent training phrase list is not empty")
                .isNotEmpty();
        boolean foundParentTrainingPhrase = hasTrainingPhrase(foundParentIntent, parentTrainingSentence);
        assertThat(foundParentTrainingPhrase).as("The parent IntentDefinition training phrase has been registered")
                .isTrue();
        assertThat(foundFollowUpIntent).as("Registered Intent list contains the follow-up IntentDefinition")
                .isNotNull();
        assertThat(foundFollowUpIntent.getTrainingPhrasesList()).as("Follow-up Intent training phrase list is not " +
                "empty").isNotEmpty();
        boolean foundFollowUpTrainingPhrase = hasTrainingPhrase(foundFollowUpIntent, followUpTrainingSentence);
        assertThat(foundFollowUpTrainingPhrase).as("The follow-up IntentDefinition training phrase has been " +
                "registered").isTrue();
        assertThat(foundParentIntent.getFollowupIntentInfoCount()).as("Parent Intent has 1 follow-up Intent")
                .isEqualTo(1);
        Intent.FollowupIntentInfo followUpIntentInfo = foundParentIntent.getFollowupIntentInfo(0);
        assertThat(followUpIntentInfo).as("Not null follow-up info").isNotNull();
        assertThat(followUpIntentInfo.getFollowupIntentName()).as("Valid follow-up Intent name").isEqualTo
                (foundFollowUpIntent.getName());
        assertThat(foundFollowUpIntent.getParentFollowupIntentName()).as("Valid parent Intent for follow-up Intent")
                .isEqualTo(foundParentIntent.getName());
    }

    @Test(expected = DialogFlowException.class)
    public void registerIntentDefinitionAlreadyRegistered() {
        api = getValidDialogFlowApi();
        registeredIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        String intentName = "TestAlreadyDefined";
        registeredIntentDefinition.setName(intentName);
        registeredIntentDefinition.getTrainingSentences().add("test");
        registeredIntentDefinition.getTrainingSentences().add("test jarvis");
        api.registerIntentDefinition(registeredIntentDefinition);
        api.registerIntentDefinition(registeredIntentDefinition);
    }

    @Test(expected = NullPointerException.class)
    public void createTrainingPhraseNullSentence() {
        api = getValidDialogFlowApi();
        api.createTrainingPhrase(null, Arrays.asList(VALID_OUT_CONTEXT));
    }

    @Test(expected = NullPointerException.class)
    public void createTrainingPhraseNullContext() {
        api = getValidDialogFlowApi();
        api.createTrainingPhrase(VALID_TRAINING_SENTENCE_WITHOUT_CONTEXT_PARAMETER, null);
    }

    @Test
    public void createTrainingPhraseEmptyContextList() {
        api = getValidDialogFlowApi();
        Intent.TrainingPhrase trainingPhrase = api.createTrainingPhrase
                (VALID_TRAINING_SENTENCE_WITHOUT_CONTEXT_PARAMETER, Collections.emptyList());
        assertThat(trainingPhrase).as("Not null training phrase").isNotNull();
        List<Intent.TrainingPhrase.Part> parts = trainingPhrase.getPartsList();
        assertThat(parts).as("Not null part list").isNotNull();
        assertThat(parts).as("Part list contains 1 element").hasSize(1);
        assertThat(parts.get(0)).as("Not null part").isNotNull();
        assertThat(parts.get(0).getText()).as("Valid part text").isEqualTo
                (VALID_TRAINING_SENTENCE_WITHOUT_CONTEXT_PARAMETER);
    }

    @Test
    public void createTrainingPhraseValidContextListNoParameterInSentence() {
        api = getValidDialogFlowApi();
        Intent.TrainingPhrase trainingPhrase = api.createTrainingPhrase
                (VALID_TRAINING_SENTENCE_WITHOUT_CONTEXT_PARAMETER, Arrays.asList(VALID_OUT_CONTEXT));
        assertThat(trainingPhrase).as("Not null training phrase").isNotNull();
        List<Intent.TrainingPhrase.Part> parts = trainingPhrase.getPartsList();
        assertThat(parts).as("Not null part list").isNotNull();
        assertThat(parts).as("Part list contains 1 element").hasSize(1);
        assertThat(parts.get(0)).as("Not null part").isNotNull();
        assertThat(parts.get(0).getText()).as("Valid part text").isEqualTo
                (VALID_TRAINING_SENTENCE_WITHOUT_CONTEXT_PARAMETER);
    }

    @Test
    public void createTrainingPhraseValidContextListParameterInSentence() {
        api = getValidDialogFlowApi();
        Intent.TrainingPhrase trainingPhrase = api.createTrainingPhrase
                (VALID_TRAINING_SENTENCE_WITH_CONTEXT_PARAMETER, Arrays.asList(VALID_OUT_CONTEXT));
        assertThat(trainingPhrase).as("Not null training phrase").isNotNull();
        List<Intent.TrainingPhrase.Part> parts = trainingPhrase.getPartsList();
        assertThat(parts).as("Not null part list").isNotNull();
        assertThat(parts).as("Part list contains 3 elements").hasSize(3);
        assertThat(parts.get(0)).as("Not null part 0").isNotNull();
        assertThat(parts.get(0).getText()).as("Valid part 0 content").isEqualTo("I love the ");
        Intent.TrainingPhrase.Part part1 = parts.get(1);
        assertThat(part1).as("Not null part 1").isNotNull();
        assertThat(part1.getText()).as("Valid part 1 content").isEqualTo("test");
        // The translation is done by the EntityMapper
        assertThat(part1.getEntityType()).as("Valid part 1 entity type").isEqualTo("@sys.any");
        assertThat(part1.getAlias()).as("Valid part 1 alias").isEqualTo(VALID_OUT_CONTEXT.getParameters().get(0)
                .getName());
        assertThat(parts.get(2)).as("Not null part 2").isNotNull();
        assertThat(parts.get(2).getText()).as("Valid part 2 content").isEqualTo(" monkey head");
    }

    @Test
    public void createTrainingPhraseValidContextListParameterLastInSentence() {
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName("ValidContext2");
        ContextParameter param = IntentFactory.eINSTANCE.createContextParameter();
        param.setName("param");
        param.setTextFragment("head");
        BaseEntityDefinition entityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        entityDefinition.setEntityType(EntityType.ANY);
        param.setEntity(entityDefinition);
        context.getParameters().add(param);
        api = getValidDialogFlowApi();
        Intent.TrainingPhrase trainingPhrase = api.createTrainingPhrase
                (VALID_TRAINING_SENTENCE_WITH_CONTEXT_PARAMETER, Arrays.asList(context));
        assertThat(trainingPhrase).as("Not null training phrase").isNotNull();
        List<Intent.TrainingPhrase.Part> parts = trainingPhrase.getPartsList();
        assertThat(parts).as("Not null part list").isNotNull();
        assertThat(parts).as("Part list contains 2 elements").hasSize(2);
        assertThat(parts.get(0)).as("Not null part 0").isNotNull();
        assertThat(parts.get(0).getText()).as("Valid part 0 content").isEqualTo("I love the test monkey ");
        Intent.TrainingPhrase.Part part1 = parts.get(1);
        assertThat(part1).as("Not null part 1").isNotNull();
        assertThat(part1.getText()).as("Valid part 1 content").isEqualTo("head");
        // The translation is done by the EntityMapper
        assertThat(part1.getEntityType()).as("Valid part 1 entity type").isEqualTo("@sys.any");
        assertThat(part1.getAlias()).as("Valid part 1 alias").isEqualTo("param");
    }

    @Test(expected = NullPointerException.class)
    public void createTrainingPhraseNullNameContextParameterInSentence() {
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName("ValidContext2");
        ContextParameter param = IntentFactory.eINSTANCE.createContextParameter();
        param.setTextFragment("head");
        BaseEntityDefinition entityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        entityDefinition.setEntityType(EntityType.ANY);
        param.setEntity(entityDefinition);
        context.getParameters().add(param);
        api = getValidDialogFlowApi();
        Intent.TrainingPhrase trainingPhrase = api.createTrainingPhrase
                (VALID_TRAINING_SENTENCE_WITH_CONTEXT_PARAMETER, Arrays.asList(context));
    }

    @Test(expected = NullPointerException.class)
    public void createInContextNamesNullIntentDefinition() {
        api = getValidDialogFlowApi();
        api.createInContextNames(null);
    }

    @Test
    public void createInContextNamesIntentDefinitionEmptyInContext() {
        api = getValidDialogFlowApi();
        List<String> inContextNames = api.createInContextNames(IntentFactory.eINSTANCE.createIntentDefinition());
        assertThat(inContextNames).as("Not null context name list").isNotNull();
        assertThat(inContextNames).as("Empty in context names list").isEmpty();
    }

    @Test
    public void createInContextNamesIntentDefinitionMultipleInContexts() {
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        Context inContext1 = IntentFactory.eINSTANCE.createContext();
        inContext1.setName("InContext1");
        Context inContext2 = IntentFactory.eINSTANCE.createContext();
        inContext2.setName("InContext2");
        intentDefinition.getInContexts().add(inContext1);
        intentDefinition.getInContexts().add(inContext2);
        api = getValidDialogFlowApi();
        List<String> inContextNames = api.createInContextNames(intentDefinition);
        assertThat(inContextNames).as("Not null in context names list").isNotNull();
        assertThat(inContextNames).as("In context names list contains 2 elements").hasSize(2);
        assertThat(inContextNames.get(0)).as("Not null context name 1").isNotNull();
        assertThat(inContextNames.get(0)).as("Valid in context name 1").endsWith("InContext1");
        assertThat(inContextNames.get(1)).as("Not null context name 2").isNotNull();
        assertThat(inContextNames.get(1)).as("Valid in context name 2").endsWith("InContext2");
    }

    @Test
    public void createInContextNamesIntentDefinitionWithFollowsSet() {
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        Context inContext1 = IntentFactory.eINSTANCE.createContext();
        inContext1.setName("InContext1");
        intentDefinition.getInContexts().add(inContext1);
        IntentDefinition parentIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        parentIntentDefinition.setName("parent");
        intentDefinition.setFollows(parentIntentDefinition);
        api = getValidDialogFlowApi();
        List<String> inContextNames = api.createInContextNames(intentDefinition);
        assertThat(inContextNames).as("Not null in context names list").isNotNull();
        assertThat(inContextNames).as("In context names list contains 2 elements").hasSize(2);
        assertThat(inContextNames.get(0)).as("Not null context name 1").isNotNull();
        assertThat(inContextNames.get(0)).as("Valid in context name 1").endsWith("InContext1");
        assertThat(inContextNames.get(1)).as("Not null context name 2").isNotNull();
        assertThat(inContextNames.get(1)).as("Valid in context name 2").endsWith("parent_followUp");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createInContextNamesIntentDefinitionWithFollowsSetNullParentName() {
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        IntentDefinition parentIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setFollows(parentIntentDefinition);
        api = getValidDialogFlowApi();
        api.createInContextNames(intentDefinition);
    }

    @Test(expected = NullPointerException.class)
    public void createOutContextsNullIntentDefinition() {
        api = getValidDialogFlowApi();
        api.createOutContexts(null);
    }

    @Test
    public void createOutContextsIntentDefinitionEmptyContext() {
        api = getValidDialogFlowApi();
        List<com.google.cloud.dialogflow.v2.Context> contexts = api.createOutContexts(IntentFactory.eINSTANCE
                .createIntentDefinition());
        assertThat(contexts).as("Not null context list").isNotNull();
        assertThat(contexts).as("Empty context list").isEmpty();
    }

    @Test
    public void createOutContextsIntentDefinitionMultipleInContexts() {
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        Context outContext1 = IntentFactory.eINSTANCE.createContext();
        outContext1.setName("OutContext1");
        outContext1.setLifeSpan(2);
        Context outContext2 = IntentFactory.eINSTANCE.createContext();
        outContext2.setName("OutContext2");
        outContext2.setLifeSpan(3);
        intentDefinition.getOutContexts().add(outContext1);
        intentDefinition.getOutContexts().add(outContext2);
        api = getValidDialogFlowApi();
        List<com.google.cloud.dialogflow.v2.Context> contexts = api.createOutContexts(intentDefinition);
        assertThat(contexts).as("Not null context list").isNotNull();
        assertThat(contexts).as("Context list contains 2 elements").hasSize(2);
        assertThat(contexts.get(0)).as("Not null context 1").isNotNull();
        assertThat(contexts.get(0).getName()).as("Valid context name 1").endsWith("OutContext1");
        assertThat(contexts.get(0).getLifespanCount()).as("Valid context lifespan 1").isEqualTo(2);
        assertThat(contexts.get(1)).as("Not null context 2").isNotNull();
        assertThat(contexts.get(1).getName()).as("Valid context name 2").endsWith("OutContext2");
        assertThat(contexts.get(1).getLifespanCount()).as("Valid context lifespan 2").isEqualTo(3);
    }

    @Test
    public void createOutContextsIntentDefinitionWithFollowedBySet() {
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("parent");
        Context inContext1 = IntentFactory.eINSTANCE.createContext();
        inContext1.setName("OutContext1");
        inContext1.setLifeSpan(3);
        intentDefinition.getOutContexts().add(inContext1);
        IntentDefinition childIntentDeclaration = IntentFactory.eINSTANCE.createIntentDefinition();
        childIntentDeclaration.setName("child");
        intentDefinition.getFollowedBy().add(childIntentDeclaration);
        api = getValidDialogFlowApi();
        List<com.google.cloud.dialogflow.v2.Context> contexts = api.createOutContexts(intentDefinition);
        assertThat(contexts).as("Not null context list").isNotNull();
        assertThat(contexts).as("Context list contains 2 elements").hasSize(2);
        assertThat(contexts.get(0)).as("Not null context 1").isNotNull();
        assertThat(contexts.get(0).getName()).as("Valid context name 1").endsWith("OutContext1");
        assertThat(contexts.get(0).getLifespanCount()).as("Valid context lifespan 1").isEqualTo(3);
        assertThat(contexts.get(1)).as("Not null context 2").isNotNull();
        assertThat(contexts.get(1).getName()).as("Valid context name 2").endsWith("parent_followUp");
        // Check that the default lifespan count has been set
        assertThat(contexts.get(1).getLifespanCount()).as("Valid context lifespan 2").isEqualTo(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createOutContextsIntentDefinitionWithFollowedBySetNullParentName() {
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        IntentDefinition childIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.getFollowedBy().add(childIntentDefinition);
        api = getValidDialogFlowApi();
        api.createOutContexts(intentDefinition);
    }

    @Test(expected = NullPointerException.class)
    public void createParametersNullContextList() {
        api = getValidDialogFlowApi();
        api.createParameters(null);
    }

    @Test
    public void createParameterEmptyContextList() {
        api = getValidDialogFlowApi();
        List<Intent.Parameter> parameters = api.createParameters(Collections.emptyList());
        assertThat(parameters).as("Parameter list is empty").isEmpty();
    }

    @Test
    public void createParameterValidContext() {
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName("Context");
        context.setLifeSpan(3);
        ContextParameter param1 = IntentFactory.eINSTANCE.createContextParameter();
        param1.setName("param1");
        BaseEntityDefinition entityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        entityDefinition.setEntityType(EntityType.ANY);
        param1.setEntity(entityDefinition);
        context.getParameters().add(param1);
        api = getValidDialogFlowApi();
        List<Intent.Parameter> parameters = api.createParameters(Arrays.asList(context));
        assertThat(parameters).as("Parameter list is not null").isNotNull();
        assertThat(parameters).as("Parameter list contains 1 element").hasSize(1);
        assertThat(parameters.get(0).getDisplayName()).as("Valid parameter display name").isEqualTo(param1.getName());
        // The translation is done by the EntityMapper
        assertThat(parameters.get(0).getEntityTypeDisplayName()).as("Valid parameter entity type").isEqualTo("@sys" +
                ".any");
        assertThat(parameters.get(0).getValue()).as("Valid parameter value").isEqualTo("$" + param1.getName());
    }

    @Test(expected = NullPointerException.class)
    public void createParameterContextParameterNullEntity() {
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName("Context");
        context.setLifeSpan(3);
        ContextParameter param1 = IntentFactory.eINSTANCE.createContextParameter();
        param1.setName("param1");
        context.getParameters().add(param1);
        api = getValidDialogFlowApi();
        // The EntityMapper should throw a NullPointerException
        api.createParameters(Arrays.asList(context));
    }

    @Test(expected = NullPointerException.class)
    public void createParameterContextParameterNullParameterName() {
        Context context = IntentFactory.eINSTANCE.createContext();
        context.setName("Context");
        context.setLifeSpan(3);
        ContextParameter param1 = IntentFactory.eINSTANCE.createContextParameter();
        BaseEntityDefinition entityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        entityDefinition.setEntityType(EntityType.ANY);
        param1.setEntity(entityDefinition);
        context.getParameters().add(param1);
        api = getValidDialogFlowApi();
        api.createParameters(Arrays.asList(context));
    }

    @Test(expected = NullPointerException.class)
    public void deleteIntentDefinitionNullIntentDefinition() {
        api = getValidDialogFlowApi();
        api.deleteIntentDefinition(null);
    }

    @Test(expected = NullPointerException.class)
    public void deleteIntentDefinitionNullName() {
        api = getValidDialogFlowApi();
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        api.deleteIntentDefinition(intentDefinition);
    }

    @Test
    public void deleteIntentDefinitionNotRegisteredIntent() {
        api = getValidDialogFlowApi();
        String intentName = "TestDeleteNotRegistered";
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName(intentName);
        int registeredIntentsCount = api.getRegisteredIntents().size();
        api.deleteIntentDefinition(intentDefinition);
        assertThat(api.getRegisteredIntents()).as("Registered Intents list has not changed").hasSize
                (registeredIntentsCount);
    }

    @Test
    public void deleteIntentDefinitionRegisteredIntentDefinition() {
        api = getValidDialogFlowApi();
        String intentName = "TestDeleteRegistered";
        registeredIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        registeredIntentDefinition.setName(intentName);
        api.registerIntentDefinition(registeredIntentDefinition);
        api.deleteIntentDefinition(registeredIntentDefinition);
        Intent foundIntent = null;
        for (Intent intent : api.getRegisteredIntents()) {
            if (intent.getDisplayName().equals(intentName)) {
                foundIntent = intent;
            }
        }
        assertThat(foundIntent).as("The Intent has been removed from the registered Intents").isNull();

    }

    @Test
    public void createSessionValidApi() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        checkDialogFlowSession(session, VALID_PROJECT_ID, "sessionID");
    }

    @Test
    public void createSessionWithContextProperty() {
        Configuration configuration = buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        configuration.addProperty(RuntimeContexts.VARIABLE_TIMEOUT_KEY, 10);
        api = new DialogFlowApi(jarvisCore, configuration);
        JarvisSession session = api.createSession("sessionID");
        checkDialogFlowSession(session, VALID_PROJECT_ID, "sessionID");
        softly.assertThat(session.getRuntimeContexts().getVariableTimeout()).as("Valid RuntimeContexts variable timeout")
                .isEqualTo(10);
    }

    @Test(expected = NullPointerException.class)
    public void mergeLocalSessionInDialogFlowNullSession() {
        api = getValidDialogFlowApi();
        api.mergeLocalSessionInDialogFlow(null);
    }

    @Test
    public void mergeLocalSessionInDialogFlowEmptySession() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession(UUID.randomUUID().toString());
        RecognizedIntent recognizedIntent = api.getIntent("Hello", session);
        assertThat(recognizedIntent.getOutContextInstances()).as("Empty out context list").isEmpty();
    }

    @Test
    public void mergeLocalSessionInDialogFlowNotEmptySessionNotExistingOutContext() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession(UUID.randomUUID().toString());
        session.getRuntimeContexts().setContextValue("context", 5, "key", "value");
        RecognizedIntent recognizedIntent = api.getIntent("Hello", session);
        assertThat(recognizedIntent.getOutContextInstances()).as("Empty out context list").isEmpty();
    }

    @Test
    public void mergeLocalSessionInDialogFlowNotEmptySessionExistingOutContext() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession(UUID.randomUUID().toString());
        registeredIntentDefinition = VALID_INTENT_DEFINITION_WITH_OUT_CONTEXT;
        /*
         * Do not register it in DialogFlow, we are just testing that the value has been set, no need to waste time
         * accessing the remote API.
         */
        jarvisCore.getEventDefinitionRegistry().registerEventDefinition(registeredIntentDefinition);
        String validContextName = VALID_OUT_CONTEXT.getName();
        String validParameterName = VALID_OUT_CONTEXT.getParameters().get(0).getName();
        session.getRuntimeContexts().setContextValue(validContextName, 5, validParameterName, "test");
        RecognizedIntent recognizedIntent = api.getIntent("Hello", session);
        assertThat(recognizedIntent.getOutContextInstances()).as("Out context list contains 1 element").hasSize(1);
        ContextInstance contextInstance = recognizedIntent.getOutContextInstances().get(0);
        assertThat(contextInstance).as("Not null ContextInstance").isNotNull();
        /*
         * Check the lifespan is equal to 4, the input string count as an interaction and decreases the lifespan count.
         */
        assertThat(contextInstance.getLifespanCount()).as("Valid ContextInstance lifespan count").isEqualTo(4);
        Context context = contextInstance.getDefinition();
        assertThat(context).as("Not null Context definition").isNotNull();
        assertThat(context.getName()).as("Valid Context definition name").isEqualTo(VALID_OUT_CONTEXT.getName());
        List<ContextParameterValue> contextParameterValues = contextInstance.getValues();
        assertThat(contextParameterValues).as("ContextInstance contains 1 parameter value").hasSize(1);
        ContextParameterValue contextParameterValue = contextParameterValues.get(0);
        assertThat(contextParameterValue.getValue()).as("Valid parameter value").isEqualTo("test");
        ContextParameter contextParameter = contextParameterValue.getContextParameter();
        assertThat(contextParameter).as("Not null Context parameter definition").isNotNull();
        assertThat(contextParameter.getName()).as("Valid Context parameter definition name").isEqualTo
                (validParameterName);
    }

    @Test(expected = DialogFlowException.class)
    public void shutdownAlreadyShutdown() {
        api = getValidDialogFlowApi();
        api.shutdown();
        api.shutdown();
    }

    @Test
    public void shutdown() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        api.shutdown();
        softly.assertThat(api.isShutdown()).as("DialogFlow API is shutdown").isTrue();
        assertThatExceptionOfType(DialogFlowException.class).isThrownBy(() -> api.getIntent("test", session))
                .withMessage("Cannot extract an Intent from the provided input, the DialogFlow API is shutdown");
        assertThatExceptionOfType(DialogFlowException.class).isThrownBy(() -> api.createSession("sessionID"))
                .withMessage
                        ("Cannot create a new Session, the DialogFlow API is shutdown");
    }

    @Test
    public void getIntentValidSession() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent(SAMPLE_INPUT, session);
        IntentDefinition intentDefinition = (IntentDefinition) intent.getDefinition();
        assertThat(intent).as("Null Intent").isNotNull();
        assertThat(intentDefinition).as("Null Intent Definition").isNotNull();
        assertThat(intentDefinition.getName()).as("Valid Intent").isEqualTo("Default Welcome Intent");
    }

    @Test(expected = DialogFlowException.class)
    public void getIntentInvalidSession() {
        api = new DialogFlowApi(jarvisCore, buildConfiguration("test", null));
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent(SAMPLE_INPUT, session);
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullSession() {
        api = getValidDialogFlowApi();
        RecognizedIntent intent = api.getIntent(SAMPLE_INPUT, null);
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullText() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent(null, session);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIntentEmptyText() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent("", session);
    }

    @Test
    public void getIntentUnknownText() {
        api = getValidDialogFlowApi();
        JarvisSession session = api.createSession("sessionID");
        RecognizedIntent intent = api.getIntent("azerty", session);
        assertThat(intent.getDefinition()).as("IntentDefinition is not null").isNotNull();
        assertThat(intent.getDefinition().getName()).as("IntentDefinition is the Default Fallback Intent").isEqualTo
                ("Default_Fallback_Intent");
    }

    @Test
    public void getIntentMultipleOutputContextNoParameters() {
        String trainingSentence = "I love the monkey head";
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestMonkeyHead");
        intentDefinition.getTrainingSentences().add(trainingSentence);
        Context outContext1 = IntentFactory.eINSTANCE.createContext();
        outContext1.setName("Context1");
        // default lifespan count
        Context outContext2 = IntentFactory.eINSTANCE.createContext();
        outContext2.setName("Context2");
        outContext2.setLifeSpan(4);
        intentDefinition.getOutContexts().add(outContext1);
        intentDefinition.getOutContexts().add(outContext2);
        api = getValidDialogFlowApi();
        try {
            api.registerIntentDefinition(intentDefinition);
        } catch (DialogFlowException e) {
            /*
             * The intent is already registered
             */
            Log.warn("The intent {0} is already registered", intentDefinition.getName());
        }
        api.trainMLEngine();
        jarvisCore.getEventDefinitionRegistry().registerEventDefinition(intentDefinition);
        /*
         * Setting this variable will delete the intent after execution. This should be done to ensure that the
         * intents are well created, but it causes intent propagation issues on the DialogFlow side (see
         * https://productforums.google.com/forum/m/#!category-topic/dialogflow/type-troubleshooting/UDokzc7mOcY)
         */
//        registeredIntentDefinition = intentDefinition;
        JarvisSession session = api.createSession(UUID.randomUUID().toString());
        RecognizedIntent recognizedIntent = api.getIntent(trainingSentence, session);
        assertThat(recognizedIntent).as("Not null recognized intent").isNotNull();
        assertThat(recognizedIntent.getDefinition()).as("Not null definition").isNotNull();
        softly.assertThat(recognizedIntent.getDefinition().getName()).as("Valid IntentDefinition").isEqualTo
                (intentDefinition.getName());
        /*
         * The ContextInstances are set, but they should not contain any value.
         */
        assertThat(recognizedIntent.getOutContextInstances()).as("Empty out context instances").hasSize(2);
        assertThat(recognizedIntent.getOutContextInstance("Context1")).as("RecognizedIntent contains Context1")
                .isNotNull();
        softly.assertThat(recognizedIntent.getOutContextInstance("Context1").getValues()).as("ContextInstance 1 does " +
                "not contain any value").isEmpty();
        ;
        assertThat(recognizedIntent.getOutContextInstance("Context2")).as("RecognizedIntent contains Context2")
                .isNotNull();
        softly.assertThat(recognizedIntent.getOutContextInstance("Context2").getValues()).as("ContextInstance 2 does " +
                "not contain any value").isEmpty();
        /*
         * Check that the lifespan counts have been properly set.
         */
        softly.assertThat(recognizedIntent.getOutContextInstance("Context1").getLifespanCount()).as("Valid " +
                "ContextInstance 1 lifespan count").isEqualTo(outContext1.getLifeSpan());
        softly.assertThat(recognizedIntent.getOutContextInstance("Context2").getLifespanCount()).as("Valid " +
                "ContextInstance 2 lifespan count").isEqualTo(outContext2.getLifeSpan());
    }

    @Test
    public void getIntentMultipleOutputContextParameters() {
        /*
         * Change the training sentence to avoid deleted intent definition matching (deleted intents can take some
         * time to be completely removed from the DialogFlow Agent, see https://discuss.api
         * .ai/t/intent-mismatch-issue/12042/17)
         */
        String trainingSentence = "cheese steak jimmy's";
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestCheeseSteakJimmys");
        intentDefinition.getTrainingSentences().add(trainingSentence);
        Context outContext1 = IntentFactory.eINSTANCE.createContext();
        outContext1.setName("Context1");
        ContextParameter contextParameter1 = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter1.setName("Parameter1");
        contextParameter1.setEntity(VALID_ENTITY_DEFINITION);
        contextParameter1.setTextFragment("cheese");
        outContext1.getParameters().add(contextParameter1);
        Context outContext2 = IntentFactory.eINSTANCE.createContext();
        outContext2.setName("Context2");
        ContextParameter contextParameter2 = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter2.setName("Parameter2");
        contextParameter2.setEntity(VALID_ENTITY_DEFINITION_2);
        contextParameter2.setTextFragment("steak");
        outContext2.getParameters().add(contextParameter2);
        outContext2.setLifeSpan(4);
        intentDefinition.getOutContexts().add(outContext1);
        intentDefinition.getOutContexts().add(outContext2);
        api = getValidDialogFlowApi();
        try {
            api.registerIntentDefinition(intentDefinition);
        } catch (DialogFlowException e) {
            Log.warn("The intent {0} is already registered", intentDefinition.getName());
        }
        api.trainMLEngine();
        jarvisCore.getEventDefinitionRegistry().registerEventDefinition(intentDefinition);
        /*
         * Setting this variable will delete the intent after execution. This should be done to ensure that the
         * intents are well created, but it causes intent propagation issues on the DialogFlow side (see
         * https://productforums.google.com/forum/m/#!category-topic/dialogflow/type-troubleshooting/UDokzc7mOcY)
         */
//        registeredIntentDefinition = intentDefinition;
        JarvisSession session = api.createSession(UUID.randomUUID().toString());
        RecognizedIntent recognizedIntent = api.getIntent(trainingSentence, session);
        assertThat(recognizedIntent).as("Not null recognized intent").isNotNull();
        assertThat(recognizedIntent.getDefinition()).as("Not null definition").isNotNull();
        softly.assertThat(recognizedIntent.getDefinition().getName()).as("Valid IntentDefinition").isEqualTo
                (intentDefinition.getName());
        assertThat(recognizedIntent.getOutContextInstances()).as("Valid out context instance list size").hasSize(2);

        /*
         * The first context is actually the second one defined in the IntentDefinition. DialogFlow does not provide
         * any specification on the order of the returned context. This test should be refactored to ensure that the
         * two contexts are defined without taking into account the order.
         */
        ContextInstance contextInstance2 = recognizedIntent.getOutContextInstances().get(0);
        assertThat(contextInstance2).as("Not null out context instance 2").isNotNull();
        assertThat(contextInstance2.getDefinition()).as("Not null out context instance 2 definition").isNotNull();
        softly.assertThat(contextInstance2.getDefinition().getName()).as("Valid out context instance 2 definition")
                .isEqualTo("Context2");
        assertThat(contextInstance2.getValues()).as("Out context instance 2 contains one value").hasSize(1);
        ContextParameterValue value2 = contextInstance2.getValues().get(0);
        assertThat(value2).as("Not null ContextParameterValue2").isNotNull();
        assertThat(value2.getContextParameter()).as("Not null ContextParameter2").isNotNull();
        softly.assertThat(value2.getContextParameter().getName()).as("Valid ContextParameter 2").isEqualTo
                (contextParameter2.getName());
        softly.assertThat(value2.getValue()).as("Valid ContextParameterValue 2").isEqualTo("steak");

        ContextInstance contextInstance1 = recognizedIntent.getOutContextInstances().get(1);
        assertThat(contextInstance1).as("Not null out context instance 1").isNotNull();
        assertThat(contextInstance1.getDefinition()).as("Not null out context instance 1 definition").isNotNull();
        softly.assertThat(contextInstance1.getDefinition().getName()).as("Valid out context instance 1 definition")
                .isEqualTo("Context1");
        assertThat(contextInstance1.getValues()).as("Out context instance 1 contains one value").hasSize(1);
        ContextParameterValue value1 = contextInstance1.getValues().get(0);
        assertThat(value1).as("Not null ContextParameterValue1").isNotNull();
        assertThat(value1.getContextParameter()).as("Not null ContextParameter1").isNotNull();
        softly.assertThat(value1.getContextParameter().getName()).as("Valid ContextParameter 1").isEqualTo
                (contextParameter1.getName());
        softly.assertThat(value1.getValue()).as("Valid ContextParameterValue 1").isEqualTo("cheese");
        /*
         * Check that the lifespan counts have been properly set.
         */
        softly.assertThat(recognizedIntent.getOutContextInstance("Context1").getLifespanCount()).as("Valid " +
                "ContextInstance 1 lifespan count").isEqualTo(outContext1.getLifeSpan());
        softly.assertThat(recognizedIntent.getOutContextInstance("Context2").getLifespanCount()).as("Valid " +
                "ContextInstance 2 lifespan count").isEqualTo(outContext2.getLifeSpan());
    }

    @Test
    public void getIntentInContextSetFromRuntimeContext() {
        /*
         * Change the training sentence to avoid deleted intent definition matching (deleted intents can take some
         * time to be completely removed from the DialogFlow Agent, see https://discuss.api
         * .ai/t/intent-mismatch-issue/12042/17)
         */
        String trainingSentence = "how do you turn this on?";
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestHowDoYouTurn");
        intentDefinition.getTrainingSentences().add(trainingSentence);
        String inContextName = "testincontext";
        Context inContext = IntentFactory.eINSTANCE.createContext();
        inContext.setName(inContextName);
        intentDefinition.getInContexts().add(inContext);
        api = getValidDialogFlowApi();
        try {
            api.registerIntentDefinition(intentDefinition);
        } catch (DialogFlowException e) {
            Log.warn("The intent {0} is already registered", intentDefinition.getName());
        }
        api.trainMLEngine();
        jarvisCore.getEventDefinitionRegistry().registerEventDefinition(intentDefinition);
        /*
         * Setting this variable will delete the intent after execution. This should be done to ensure that the
         * intents are well created, but it causes intent propagation issues on the DialogFlow side (see
         * https://productforums.google.com/forum/m/#!category-topic/dialogflow/type-troubleshooting/UDokzc7mOcY)
         */
//        registeredIntentDefinition = intentDefinition;
        JarvisSession session = api.createSession(UUID.randomUUID().toString());
        /*
         * Set the input context in the JarvisSession's local context. If the intent is matched the local session has
         * been successfully merged in the Dialogflow one.
         */
        session.getRuntimeContexts().setContextValue(inContextName, 5, "testKey", "testValue");
        RecognizedIntent recognizedIntent = api.getIntent(trainingSentence, session);
        assertThat(recognizedIntent).as("Not null recognized intent").isNotNull();
        assertThat(recognizedIntent.getDefinition()).as("Not null definition").isNotNull();
        softly.assertThat(recognizedIntent.getDefinition().getName()).as("Valid IntentDefinition").isEqualTo
                (intentDefinition.getName());
    }

    private boolean hasTrainingPhrase(Intent intent, String trainingPhrase) {
        for (Intent.TrainingPhrase intentTrainingPhrase : intent.getTrainingPhrasesList()) {
            for (Intent.TrainingPhrase.Part part : intentTrainingPhrase.getPartsList()) {
                if (part.getText().equals(trainingPhrase)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkDialogFlowSession(JarvisSession session, String expectedProjectId, String expectedSessionId) {
        assertThat(session).as("Not null session").isNotNull();
        assertThat(session).as("The session is a DialogFlowSession instance").isInstanceOf(DialogFlowSession.class);
        DialogFlowSession dialogFlowSession = (DialogFlowSession) session;
        assertThat(dialogFlowSession.getSessionName()).as("Not null SessionName").isNotNull();
        softly.assertThat(dialogFlowSession.getSessionName().getProject()).as("Valid session project").isEqualTo
                (expectedProjectId);
        softly.assertThat(dialogFlowSession.getSessionName().getSession()).as("Valid session name").isEqualTo
                (expectedSessionId);
    }

}