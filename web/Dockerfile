FROM amazonlinux:2.0.20190228

LABEL maintainer "uzresk"

# install packages
RUN yum -y install glibc-langpack-ja \
                   shadow-utils \
                   httpd

# set locale ja_JP.UTF-8, timezone
ENV LANG=ja_JP.utf8
ENV LC_ALL ja_JP.utf8
RUN rm -f /etc/localtime
RUN ln -fs /usr/share/zoneinfo/Asia/Tokyo /etc/localtime

ADD ./index.html /var/www/html/
RUN sed -i -e "s/Listen\ 80/Listen\ 0.0.0.0:80/" /etc/httpd/conf/httpd.conf
ADD ./http-proxy.conf /etc/httpd/conf.d/http-proxy.conf

EXPOSE 80
CMD ["/usr/sbin/httpd", "-D", "FOREGROUND"]