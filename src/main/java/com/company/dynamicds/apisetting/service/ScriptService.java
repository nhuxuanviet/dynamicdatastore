package com.company.dynamicds.apisetting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScriptService {

    private final ObjectMapper objectMapper;

    private Context createSandboxedContext(Map<String, Object> variables) {

        Context.Builder builder = Context.newBuilder("js")
                .sandbox(SandboxPolicy.CONSTRAINED)
                .allowAllAccess(false)                 //  cấm mọi truy cập host
                .allowHostClassLookup(null)            //  cấm Java.type, reflection
                .allowHostClassLoading(false)
                .allowNativeAccess(false)
                .allowCreateProcess(false)
                .allowCreateThread(false)
                .allowIO(IOAccess.NONE)                // cấm đọc ghi file, socket
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .useSystemExit(false)
                .allowInnerContextOptions(false)
                .allowExperimentalOptions(false)
                .allowValueSharing(false)             //  cấm chia sẻ giá trị giữa contexts
                .out(OutputStream.nullOutputStream())
                .err(OutputStream.nullOutputStream());

        Context context = builder.build();

        // nạp biến (dưới dạng JSON string để tránh Java object injection)
        // Truyền biến vào JS dưới dạng JSON string
        Value bindings = context.getBindings("js");
        if (variables != null) {
            for (var e : variables.entrySet()) {
                try {
                    String jsonValue = objectMapper.writeValueAsString(e.getValue());
                    bindings.putMember(e.getKey(), jsonValue);
                } catch (Exception ex) {
                    bindings.putMember(e.getKey(), String.valueOf(e.getValue()));
                }
            }
        }

        return context;
    }

    public Object execute(String scriptCode, Map<String, Object> variables) {
        try (Context context = createSandboxedContext(variables)) {
            Value result = context.eval("js", scriptCode);

            return result.toString();
        } catch (PolyglotException e) {
            log.error("[SafeScriptService] Script error: {}", e.getMessage());
            throw new RuntimeException("JavaScript error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[SafeScriptService] Execution failed: {}", e.getMessage(), e);
            throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
        }
    }
}