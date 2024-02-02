#!/bin/sh

# Using the redis-cli tool available as default in the Redis base image
# we need to create the cluster so they can coordinate with each other
# which key slots they need to hold per shard

# wait a little so we give some time for the Redis containers
# to spin up and be available on the network
sleep 5
# redis-cli doesn't support hostnames, we must match the
# container IP addresses from our docker-compose configuration.
# `--cluster-replicas 1` Will make sure that every master node will have its replica node
#echo "yes" | redis-cli --cluster create \
#  127.0.0.1:7001 \
#  127.0.0.1:7002 \
#  127.0.0.1:7003 \
#  127.0.0.1:7004 \
#  127.0.0.1:7005 \
#  127.0.0.1:7006 \
#  --cluster-replicas 1 -a admin --cluster-yes
#echo "🚀 Redis cluster ready."

echo "yes" | redis-cli --cluster create \
  redis_1:7001 \
  redis_2:7002 \
  redis_3:7003 \
  redis_4:7004 \
  redis_5:7005 \
  redis_6:7006 \
  --cluster-replicas 1 -a admin --cluster-yes
echo "🚀 Redis cluster ready."