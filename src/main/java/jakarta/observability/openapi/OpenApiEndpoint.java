package jakarta.observability.openapi;

import com.google.gson.Gson;
import io.smallrye.openapi.api.models.PathItemImpl;
import io.smallrye.openapi.api.models.PathsImpl;
import io.smallrye.openapi.api.models.info.ContactImpl;
import io.smallrye.openapi.api.models.info.InfoImpl;
import io.smallrye.openapi.api.models.info.LicenseImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.jboss.jandex.*;

import java.io.IOException;
import java.util.*;

import static jakarta.observability.openapi.OpenApiUtils.extractFromAnnotationValue;
import static jakarta.observability.openapi.OpenApiUtils.getOperationFromAnnotations;

@WebServlet(urlPatterns = "/openapi", name = "Open Api Endpoint")
@Tag(name = "nome da api", description = "descricao da api")
public class OpenApiEndpoint extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] packages = new String[]{"jakarta.observability.servlets", "jakarta.observability.openapi"};
        IndexView idxView = CustomIndexView.fromPackages(getClass().getClassLoader(), packages);

        SwaggerOpenAPImpl openApi = new SwaggerOpenAPImpl("2.0");
        openApi.setPaths(new PathsImpl());

        openApi.setInfo(new InfoImpl()
                .title("API de Observabilidade")
                .version("1.0.0")
                .description("Documentação da API de Observabilidade Operações nativas CRUD doGet, doPost e etc estão nos nomes dos paths favor ignorar Os retornos dos metodos e das annotations estão nas responses As requests são mapeadas apartir do getParameter e as saidas o setAttribute.")
                .contact(new ContactImpl()
                        .name("Support")
                        .email("suporte@exemplo.com"))
                .license(new LicenseImpl()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT")));

        Collection<ClassInfo> classes = idxView.getKnownClasses();
        for (ClassInfo classInfo : classes) {
            String fullClassNameWithPackage = classInfo.name().toString();


                ClassInfo info = idxView.getClassByName(fullClassNameWithPackage);


                for (MethodInfo methodInfo : info.methods()) {
                    List<AnnotationInstance> annotations = info.annotations();
                    List<AnnotationInstance> webServletAnnotation = annotations.stream().filter(annotation -> annotation.name().toString().toLowerCase().contains("webservlet")).toList();

                    if(webServletAnnotation.isEmpty()){
                        break;
                    }

                    AnnotationValue annotationValue = webServletAnnotation.get(0).value("urlPatterns");
                    String path = extractFromAnnotationValue(annotationValue);

                    PathItemImpl pathItem = new PathItemImpl();

                    String originalMethodName = methodInfo.name().replaceFirst(".*/", "");

                    if(originalMethodName.contains("init")){
                        continue;
                    }

                    openApi.getPaths().addPathItem(path + "/" + originalMethodName, pathItem);
                    Operation operation = getOperationFromAnnotations(methodInfo, fullClassNameWithPackage);


                    Map<String, String> parameterAndAttributeCalls = CfrDecompilerUtils.getParameterAndAttributeCalls(fullClassNameWithPackage, methodInfo.name());

                    List<Parameter> parameters = new ArrayList<>();
                    if (parameterAndAttributeCalls != null) {
                        parameterAndAttributeCalls.forEach((k, v) -> {
                            if (k.contains("getParameter")) {
                                OpenApiParameter param = new OpenApiParameter("string");
                                String paramName = v.replace("getParameter", "").replace("\"", "").trim();
                                if(!paramName.isEmpty()){
                                    param.setName(paramName);
                                    param.setIn(OpenApiParameter.In.QUERY);
                                    parameters.add(param);
                                }

                            }
                        });
                    }

                    operation.setParameters(parameters);
                    operation.addTag(classInfo.simpleName());

                    switch (originalMethodName){
                        case "doGet":
                            pathItem.setGET(operation);
                            break;
                        case "doPost":
                            pathItem.setPOST(operation);
                            break;
                        case "doPut":
                            pathItem.setPUT(operation);
                            break;
                        case "doDelete":
                            pathItem.setDELETE(operation);
                            break;
                        case "doHead":
                            pathItem.setHEAD(operation);
                            break;
                        case "doOptions":
                            pathItem.setOPTIONS(operation);
                            break;
                        case "doPatch":
                            pathItem.setPATCH(operation);
                            break;
                        case "doTrace":
                            pathItem.setTRACE(operation);
                            break;
                        default:
                            pathItem.setGET(operation);
                            break;
                    }

                }


        }

        String openApiJson = new Gson().toJson(openApi);

        // fixing types in Uppercase (POO PROBLEM)
        String[] typesInUppercase = new String[] { "QUERY", "BODY", "PATH", "HEADER", "FORMDATA"};
        for(String type : typesInUppercase){
            openApiJson = openApiJson.replaceAll(type, type.toLowerCase());
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().print(openApiJson);
    }
}