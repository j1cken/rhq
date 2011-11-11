package org.rhq.enterprise.server.plugins.alertXymon;

import java.util.ArrayList;

import org.rhq.core.domain.configuration.Configuration;

public class XymonInfo {

    private String binary;
    private String host;
    private String hostSuffix = "";
    private String prefix;
    private boolean groupResources = false;
   private String lifetime;

    final public String error;

    private final ArrayList<String> errorList = new ArrayList<String>();

    XymonInfo(String binary, String host, String prefix, boolean groupResources, String lifetime) {
        this.binary = binary;
        this.host = host;
       this.prefix = prefix;
        this.groupResources = groupResources;
       this.lifetime = lifetime;

        if (binary == null) {
            addError("Binary");
        }

        if (host == null) {
            addError("Host");
        }

        this.error = getErrors();

    }
    XymonInfo(String binary, String host, String hostSuffix, String prefix, boolean groupResources, String lifetime) {
        this(binary, host, prefix, groupResources, lifetime);
       this.hostSuffix = hostSuffix;
    }

    String getErrors() {
        if (errorList.isEmpty()) {
            return null;
        } else {
            boolean multiple = errorList.size() > 1;
            StringBuilder errorString = new StringBuilder();
            errorString.append("Missing: ");
            for (String s : errorList) {
                errorString.append(s);
                if (multiple && !(errorList.indexOf(s) == errorList.size() - 1)) {
                    errorString.append(", ");
                }
            }
            return errorString.toString();
        }
    }

    private void addError(String error) {
        errorList.add(error);
    }

    public static XymonInfo load(Configuration configuration) {
        String binary = configuration.getSimpleValue("binary", null);
        String host = configuration.getSimpleValue("host", null);
        String hostSuffix = configuration.getSimpleValue("hostSuffix", null);
        String prefix = configuration.getSimpleValue("prefix", null);
        boolean groupResources = Boolean.parseBoolean(configuration.getSimpleValue("groupResources", "false"));
       String lifetime = configuration.getSimpleValue("lifetime", null);

        return new XymonInfo(binary, host, hostSuffix, prefix, groupResources, lifetime);
    }

    /**
     * info2 has precedence over info1
     * @param info1
     * @param info2
     * @return
     */
    static XymonInfo merge(XymonInfo info1, XymonInfo info2) {
        // todo: maybe use unset (required = false)
        if (info1 == null || info2 == null) {
            throw new IllegalArgumentException("Null was passed to method!");
        }
        return new XymonInfo(
                (info2.getBinary() != null && !"".equals(info2.getBinary())) ? info2.binary : info1.binary,
                (info2.host != null && !"".equals(info2.getHost())) ? info2.host : info1.host,
                info1.getHostSuffix(),
                (info2.prefix != null && !"".equals(info2.getPrefix())) ? info2.prefix : info1.prefix,
                info2.groupResources,
                (info2.lifetime != null && !"".equals(info2.getLifetime())) ? info2.lifetime : info1.lifetime

        );
    }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer();
      sb.append("XymonInfo");
      sb.append("{groupResources=").append(groupResources);
      sb.append(", binary='").append(binary).append('\'');
      sb.append(", host='").append(host).append('\'');
      sb.append(", hostSuffix='").append(hostSuffix).append('\'');
      sb.append(", prefix='").append(prefix).append('\'');
      sb.append(", lifetime='").append(lifetime).append('\'');
      sb.append('}');
      return sb.toString();
   }

   public String getBinary() {
        return binary;
    }

    public void setBinary(String binary) {
        this.binary = binary;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

   public String getHostSuffix() {
      return hostSuffix;
   }

   public void setHostSuffix(String hostSuffix) {
      this.hostSuffix = hostSuffix;
   }

   public String getPrefix() {
        if (!"".equals(prefix) && prefix != null) {
            return new StringBuilder().append(prefix).append("-").toString();
        } else {
            return "";
        }
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isGroupResources() {
        return groupResources;
    }

    public void setGroupResources(boolean groupResources) {
        this.groupResources = groupResources;
    }

   public String getLifetime() {
      return lifetime;
   }

   public void setLifetime(String lifetime) {
      this.lifetime = lifetime;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      XymonInfo xymonInfo = (XymonInfo) o;

      if (groupResources != xymonInfo.groupResources) return false;
      if (binary != null ? !binary.equals(xymonInfo.binary) : xymonInfo.binary != null) return false;
      if (host != null ? !host.equals(xymonInfo.host) : xymonInfo.host != null) return false;
      if (hostSuffix != null ? !hostSuffix.equals(xymonInfo.hostSuffix) : xymonInfo.hostSuffix != null) return false;
      if (prefix != null ? !prefix.equals(xymonInfo.prefix) : xymonInfo.prefix != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = binary != null ? binary.hashCode() : 0;
      result = 31 * result + (host != null ? host.hashCode() : 0);
      result = 31 * result + (hostSuffix != null ? hostSuffix.hashCode() : 0);
      result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
      result = 31 * result + (groupResources ? 1 : 0);
      return result;
   }
}

