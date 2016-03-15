package com.cie;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.log4j.Logger;

public class TouchPointUtils {

    private static final Logger log = Logger.getLogger(TouchPointUtils.class.getName());

    public static void addArrestData(Connection conn, long orgId, long etoSubjectId,
                                     Date arrestedDate, String arrestedAgency,
                                     Arrestee arrestedInfo, int confidence) {
        log.info("Adding arrest data into ETO...");
        try {
            int siteId = getSiteId(orgId);
            int programId = getIntakeProgramId(orgId);
            if (siteId == 0 || programId == 0) {
                return;
            }

            // Create REST client to access ETO's services.
            Client client = Client.create();

            // Retrieve SSO authentication token.
            log.info("Retrieving SSO authentication token...\n");
            WebResource webResource = client.resource("https://services.etosoftware.com/API/Security.svc/SSOAuthenticate/");
            String credential = "{\"security\":{\"Email\":\"minh@ciesandiego.org\",\"Password\":\"m1nh@c13\"}}";

            ClientResponse response = webResource.type(MediaType.valueOf("application/json"))
                                      .post(ClientResponse.class, credential);
            if (response.getStatus() != 200) {
                log.error("Authentication failed: HTTP error code: " +
                          response.getStatus() + ", " + response.toString());
                return;
            }

            String resp = response.getEntity(String.class);
            String token = parseAuthResponse(resp);
            if (token == null) {
                log.error("Failed to get SSO authentication token");
                return;
            }

            // Retrieve the enterprise key.
            log.info("Retrieving the enteprise key...\n");
            webResource = client.resource("https://services.etosoftware.com/API/Security.svc/GetSSOEnterprises/" + token);
            response = webResource.type(MediaType.valueOf("application/json"))
                       .get(ClientResponse.class);
            if (response.getStatus() != 200) {
                log.error("Failed to get the enteprise key: HTTP error code: " +
                          response.getStatus() + ", " + response.toString());
                return;
            }

            resp = response.getEntity(String.class);
            String key = parseEnterpriseKey(resp);
            if (key == null) {
                log.error("Failed to get the enteprise key");
                return;
            }

            // Sign on to the site.
            log.info("Signing on site...\n");
            webResource = client.resource("https://services.etosoftware.com/API/Security.svc/SSOSiteLogin/" + siteId + "/" + key + "/" + token + "/" + 8);
            response = webResource.type(MediaType.valueOf("application/json"))
                       .get(ClientResponse.class);
            if (response.getStatus() != 200) {
                log.error("Failed to sign on site: HTTP error code: " +
                          response.getStatus() + ", " + response.toString());
                return;
            }

            resp = response.getEntity(String.class);
            String securityToken = parseSecurityToken(resp);
            if (securityToken == null) {
                log.error("Failed to get security token");
                return;
            }

            // ETO Arrest TouchPoint.
            JSONObject input = new JSONObject();
            input.put("TouchPointID", new Integer(52));
            input.put("SubjectID", etoSubjectId);
            Date now = new Date();
            input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
            input.put("ProgramID", new Integer(programId));

            // TouchPoint response elements.
            JSONArray respElements = new JSONArray();

            // Confidence level.
            String confidenceStr = confidence + "%";
            respElements.add(createTextRespElement(779, confidenceStr));

            // Date of arrest.
            JSONObject ele = new JSONObject();
            ele.put("ElementID", new Integer(769));
            ele.put("ElementType", new Integer(9));
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            ele.put("Value", sdf.format(arrestedDate));
            respElements.add(ele);

            // Arresting agency.
            respElements.add(createTextRespElement(770, arrestedAgency));

            // Charge codes (max = 10).
            int numCharges = arrestedInfo.charges.size();
            if (numCharges > 10) {
                numCharges = 10;
            }

            for (int i = 0; i < numCharges; i++) {
                int codeId = 782 + (i * 2);
                int codeDescId = 783 + (i * 2);
                OffenseCode oc = arrestedInfo.charges.get(i);

                // Retrieve charge description.
                String codeDesc = "";
                DbOffenseCode dboc = null;
                String tmp = oc.code;
                if (tmp.contains("(A)(B)")) { // Multiple codes.
                    String code1 = tmp.replace("(B)", "");
                    dboc = DbOffenseCode.findByCode(conn, code1);
                    if (dboc != null) {
                        respElements.add(createTextRespElement(codeId, tmp));
                        respElements.add(createTextRespElement(codeDescId, dboc.description));
                    }
                    else {
                        respElements.add(createTextRespElement(codeId, tmp));
                    }
                }
                else {
                    dboc = DbOffenseCode.findByCode(conn, tmp);
                    if (dboc != null) {
                        respElements.add(createTextRespElement(codeId, tmp));
                        respElements.add(createTextRespElement(codeDescId, dboc.description));
                    }
                    else {
                        respElements.add(createTextRespElement(codeId, tmp));
                    }
                }
            }

            // Add response elements.
            input.put("ResponseElements", respElements);

            // Wrap request JSON string.
            String jsonStr = input.toString("TouchPointResponse", input);
            String inputStr = "{" + jsonStr + "}";

            // @debug.
            log.info(inputStr + "\n");

            // Post request.
            response = postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                   key, securityToken, inputStr);
            if (response.getStatus() != 200) {
                log.error(response.toString());
            }
            else {
                // Parse response.
                Long arrestRespId = parseResponse(response, "AddTouchPointResponseResult", "TouchPointResponseID");
                log.info("Arrest Response ID: " + arrestRespId + "\n");
            }
        }
        catch (Exception ex) {
            log.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static int getSiteId(long orgId) {
        int etoSiteId = 0;
        switch ((int)orgId) {
            case 2: // Father Joe's.
                etoSiteId = 77;
                break;
            case 3: // P.A.T.H.
                etoSiteId = 79;
                break;
            case 5: // SD EMS/Fire Rescue.
                etoSiteId = 78;
                break;
            case 7: // ElderHelp.
                etoSiteId = 84;
                break;
            case 8: // Serving Seniors.
                etoSiteId = 82;
                break;
            case 9: // MoW.
                etoSiteId = 85;
                break;
            case 10: // St. Paul's.
                etoSiteId = 86;
                break;
            case 11: // Alpha Project.
                etoSiteId = 87;
                break;
            case 13: // Catholic Charities.
                etoSiteId = 88;
                break;
            case 14: // Family Health Centers.
                etoSiteId = 90;
                break;
            default:
                log.error("Invalid organization ID: " + orgId);
                break;
        }
        return etoSiteId;
    }

    private static int getIntakeProgramId(long orgId) {
        int etoProgramId = 0;
        switch ((int)orgId) {
            case 2: // Father Joe's.
                etoProgramId = 746;
                break;
            case 3: // P.A.T.H.
                etoProgramId = 747;
                break;
            case 5: // SD EMS/Fire Rescue.
                etoProgramId = 748;
                break;
            case 7: // ElderHelp.
                etoProgramId = 766;
                break;
            case 8: // Serving Seniors.
                etoProgramId = 768;
                break;
            case 9: // MoW.
                etoProgramId = 767;
                break;
            case 10: // St. Paul's.
                etoProgramId = 769;
                break;
            case 11: // Alpha Project.
                etoProgramId = 782;
                break;
            case 13: // Catholic Charities.
                etoProgramId = 786;
                break;
            case 14: // Family Health Centers.
                etoProgramId = 789;
                break;
            default:
                log.error("Invalid organization ID: " + orgId);
                break;
        }
        return etoProgramId;
    }

    private static JSONObject createTextRespElement(int elementId, String respText) {
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(elementId));
        ele.put("ElementType", new Integer(5));
        ele.put("Value", respText);
        return ele;
    }

    private static String parseAuthResponse(String resp) {
        String token = null;
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject jsonObj = (JSONObject)obj;

            JSONObject result = (JSONObject)jsonObj.get("SSOAuthenticateResult");
            log.info(result.toString() + "\n");
            long status = (Long)result.get("AuthStatusCode");
            log.info("status: " + status);
            token = (String)result.get("SSOAuthToken");
            log.info("token: " + token + "\n");
        }
        catch (ParseException pe) {
            pe.printStackTrace();
        }
        return token;
    }

    private static String parseEnterpriseKey(String resp) {
        String key = null;
        try {
            JSONParser parser = new JSONParser();
            JSONArray arr = (JSONArray)parser.parse(resp);

            JSONObject result = (JSONObject)arr.get(0);
            key = (String)result.get("Key");
            log.info("Enterprise Key: " + key);
            String value = (String)result.get("Value");
            log.info("Enterprise Value: " + value + "\n");
        }
        catch (ParseException pe) {
            pe.printStackTrace();
        }
        return key;
    }

    private static String parseSecurityToken(String resp) {
        String token = null;
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            token = (String)obj;
            log.info("Security token: " + token + "\n");
        }
        catch (ParseException pe) {
            pe.printStackTrace();
        }
        return token;
    }

    private static ClientResponse postRequest(String uri, String key, String securityToken,
                                              String inputStr) throws Exception {
        Client client = Client.create();
        WebResource webResource = client.resource(uri);
        ClientResponse response = webResource.type(MediaType.valueOf("application/json"))
                                  .header("enterpriseGuid", key)
                                  .header("securityToken", securityToken)
                                  .post(ClientResponse.class, inputStr);
        return response;
    }

    private static Long parseResponse(ClientResponse response, String resultString,
                                      String idString) throws Exception {
        Long responseId = null;
        String resp = response.getEntity(String.class);

        // @debug.
        log.debug("Response from server:");
        log.debug(resp + "\n");

        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject)parser.parse(resp);
        JSONObject result = (JSONObject)jsonObj.get(resultString);
        responseId = (Long)result.get(idString);
        return responseId;
    }
}