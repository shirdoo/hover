import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Expose a CLI to work with hover
 * Created by pallav.kothari on 5/4/17.
 */
@Slf4j
@Data
public class HoverCli {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Parameter(names = {"-u", "--username"}, required = true)
    private String username;

    @Parameter(names = {"-p", "--password"}, required = true, password = true)
    private String password;

    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help;

    public static void main(String[] args) {
        HoverCli cli = new HoverCli();

        ListDomainsCommand listDomainsCommand = new ListDomainsCommand();
        ListCnamesCommand listCnamesCommand = new ListCnamesCommand();
        AddCnameCommand addCnameCommand = new AddCnameCommand();
        DeleteCnameCommand deleteCnameCommand = new DeleteCnameCommand();
        UpdateCnameCommand updateCnameCommand = new UpdateCnameCommand();

        JCommander jc = new JCommander.Builder()
                .addObject(cli)
                .addCommand(ListDomainsCommand.LS_DOMAINS, listDomainsCommand)
                .addCommand(ListCnamesCommand.LS_CNAMES, listCnamesCommand)
                .addCommand(AddCnameCommand.ADD_CNAME, addCnameCommand)
                .addCommand(DeleteCnameCommand.DELETE_CNAME, deleteCnameCommand)
                .addCommand(UpdateCnameCommand.UPDATE_CNAME, updateCnameCommand)
                .build();
        try {
            jc.parse(args);
            if (cli.help) {
                jc.usage();
                return;
            }

            HoverApi api = new HoverApi(cli.getUsername(), cli.getPassword()).login();

            String command = Preconditions.checkNotNull(jc.getParsedCommand());

            switch (command) {
                case ListDomainsCommand.LS_DOMAINS:
                    HoverApi.Domains domains = api.getDomains();
                    System.out.println(GSON.toJson(domains.getDomains()));
                    break;
                case ListCnamesCommand.LS_CNAMES:
                    String domain = listCnamesCommand.getDomain();
                    HoverApi.Domains domainsWithDns = api.getDomainsWithDns(domain);
                    System.out.println(GSON.toJson(domainsWithDns.getDomains().get(0).getEntries()));
                    break;
                case AddCnameCommand.ADD_CNAME:
                    HoverApi.DnsEntry dns = new HoverApi.DnsEntry();    // type is always CNAME
                    dns.setName(addCnameCommand.getName());
                    dns.setDnsTarget(addCnameCommand.getDnsTarget());
                    try {
                        String resp = api.addDnsEntry(addCnameCommand.getDomain(), dns);
                        System.out.println(resp);
                    } catch (IllegalStateException ise) {
                        System.err.println(ise.getMessage());
                        System.exit(1);
                    }
                    break;
                case DeleteCnameCommand.DELETE_CNAME:
                    System.out.println(api.deleteDnsEntry(deleteCnameCommand.getId()));
                    break;
                case UpdateCnameCommand.UPDATE_CNAME:
                    dns = new HoverApi.DnsEntry();
                    dns.setName(updateCnameCommand.getName());
                    dns.setDnsTarget(updateCnameCommand.getDnsTarget());
                    try {
                        System.out.println(api.updateDnsTarget(updateCnameCommand.getDomain(), dns));
                    } catch (IllegalStateException ise) {
                        System.err.println(ise.getMessage());
                        System.exit(1);
                    }
                    break;
                default:
                    System.err.println("**** unrecognized command " + command);
                    throw new Error();
            }

        } catch (Exception e) {
            jc.usage();
            throw new RuntimeException(e);
        }
    }

    @Parameters(commandDescription = "list domains")
    private static class ListDomainsCommand {
        public static final String LS_DOMAINS = "ls:domains";
    }

    @Parameters(commandDescription = "list cnames") @Data
    private static class ListCnamesCommand {
        public static final String LS_CNAMES = "ls:cnames";

        @Parameter(names = {"--domain", "-d"}, description = "the domain for which you want cnames", required = true)
        private String domain;
    }

    @Parameters(commandDescription = "add cname record") @Data
    private static class AddCnameCommand {
        public static final String ADD_CNAME = "add:cname";

        @Parameter(names = {"--domain", "-d"}, description = "the domain for which you want to add this cname", required = true)
        private String domain;

        @Parameter(names = {"--subdomain", "-s"}, description = "the subdomain you're adding a cname record for", required = true)
        private String name;

        @Parameter(names = {"--target", "-t"}, description = "the DNS target for this cname record", required = true)
        private String dnsTarget;
    }

    @Parameters(commandDescription = "delete cname record") @Data
    private static final class DeleteCnameCommand {
        public static final String DELETE_CNAME = "rm:cname";
        @Parameter(names = {"--dns-id", "-id"}, description = "the dns id of the record you want to delete", required = true)
        private String id;
    }

    @Parameters(commandDescription = "Update cname record") @Data
    private static final class UpdateCnameCommand {
        public static final String UPDATE_CNAME = "update:cname";
        @Parameter(names = {"--domain", "-d"}, description = "the domain for which you want to add this cname", required = true)
        private String domain;

        @Parameter(names = {"--subdomain", "-s"}, description = "the subdomain you're adding a cname record for", required = true)
        private String name;

        @Parameter(names = {"--target", "-t"}, description = "the DNS target to update the cname record to", required = true)
        private String dnsTarget;
    }

}
