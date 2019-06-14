FROM amazonlinux:2.0.20190228

LABEL maintainer "uzresk"

# install packages
RUN yum -y install glibc-langpack-ja
RUN yum -y install shadow-utils
RUN yum -y install httpd

ADD ./http-proxy.conf /etc/httpd/conf.d/http-proxy.conf
ADD ./index.html /var/www/html/

# set locale ja_JP.UTF-8, timezone
ENV LANG=ja_JP.utf8
ENV LC_ALL ja_JP.utf8
RUN rm -f /etc/localtime
RUN ln -fs /usr/share/zoneinfo/Asia/Tokyo /etc/localtime

RUN sed -i -e "s/Listen\ 80/Listen\ 0.0.0.0:80/" /etc/httpd/conf/httpd.conf
RUN sed -i -e "s/ErrorLog\ \"logs\/error_log\"/ErrorLog\ \"\/dev\/stdout\"/" /etc/httpd/conf/httpd.conf
RUN sed -i -e "s/CustomLog\ \"logs\/access_log\"/CustomLog\ \"\/dev\/stdout\"/" /etc/httpd/conf/httpd.conf

EXPOSE 80
CMD ["/usr/sbin/httpd", "-D", "FOREGROUND"]
