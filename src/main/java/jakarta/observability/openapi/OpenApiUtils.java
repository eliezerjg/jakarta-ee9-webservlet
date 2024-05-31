package jakarta.observability.openapi;

import io.smallrye.openapi.api.models.OperationImpl;
import io.smallrye.openapi.api.models.examples.ExampleImpl;
import io.smallrye.openapi.api.models.media.ContentImpl;
import io.smallrye.openapi.api.models.media.MediaTypeImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.responses.APIResponsesImpl;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpenApiUtils {
    public static String extractFromAnnotationValue(AnnotationValue annotationValue){
        return (annotationValue + "").replaceAll(".*\\[\"(.*?)\"\\].*", "$1");
    }

    public static Operation getOperationFromAnnotations(MethodInfo methodInfo) {
        OperationImpl operation = new OperationImpl();
        List<AnnotationInstance> annotations = methodInfo.annotations();

        for (AnnotationInstance annotation : annotations) {
            if (annotation.name().toString().toLowerCase().contains("tag")) {

                AnnotationValue name = annotation.value("name");
                if (name != null) {
                    List<String> tags = new ArrayList<String>();

                    if(operation.getTags() != null){
                        tags.addAll(operation.getTags());
                    }

                    tags.add(name.asString());
                    operation.setTags(tags);
                }

                AnnotationValue description = annotation.value("description");
                if (description != null) {
                    List<String> tags = new ArrayList<String>();

                    if(operation.getTags() != null){
                        tags.addAll(operation.getTags());
                    }
                    tags.add(description.asString());
                    operation.setTags(tags);
                }
            }
            if (annotation.name().toString().toLowerCase().contains("operation")) {

                AnnotationValue operationId = annotation.value("operationId");
                if (operationId != null) {
                    operation.setSummary(operationId.asString());
                }

                AnnotationValue summary = annotation.value("summary");
                if (summary != null) {
                    operation.setSummary(summary.asString());
                }

                AnnotationValue description = annotation.value("description");
                if (description != null) {
                    operation.setDescription(description.asString());
                }
            }

            if (annotation.name().toString().toLowerCase().contains("apiresponses")) {
                AnnotationValue responses = annotation.value();
                if (responses != null) {
                    APIResponses apiResponses = new APIResponsesImpl();
                    for (AnnotationInstance response : responses.asNestedArray()) {
                        APIResponse apiResponse = new APIResponseImpl();
                        AnnotationValue responseCode = response.value("responseCode");
                        AnnotationValue responseDescription = response.value("description");
                        AnnotationValue content = response.value("content");
                        apiResponse.setDescription(responseDescription.asString());
                        if (content != null) {
                                    // To-DO: finish this implementation getting by the content


                                    ContentImpl contentImpl = new ContentImpl();
                                    SchemaImpl schemaImpl = new SchemaImpl();
                                    schemaImpl.setType(Schema.SchemaType.OBJECT);

                                    // for examples each ref should have a component example
                                    schemaImpl.setRef(UUID.randomUUID().toString());

                                    Example example = new ExampleImpl().description("descricao").value("{'x' : 'y'}").summary("summary").externalValue("external");
                                    contentImpl.addMediaType("application/json", new MediaTypeImpl().schema(schemaImpl).addExample("1", example) );


                            apiResponse.setContent(contentImpl);
                        }

                        apiResponses.addAPIResponse(responseCode.asString(), apiResponse);

                    }
                    operation.setResponses(apiResponses);
                }
            }
        }

        if(operation.getResponses() == null) {
            APIResponse apiResponse = new APIResponseImpl();
            apiResponse.setDescription("Não documentado");
            operation.setResponses(new APIResponsesImpl().addAPIResponse("200", apiResponse) );
        }

        return operation;
    }
}
