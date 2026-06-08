const { celebrate, Joi } = require('celebrate');

const isBookingRequest = celebrate({
  body: Joi.object().keys({
    flightDate: Joi.string().pattern(/^\d{4}-\d{2}-\d{2}$/).required(),
    flightNo: Joi.string().length(6).required(),
    fareConditions: Joi.string().valid('Economy', 'Comfort', 'Business').required(),
    passengerId: Joi.string().required(),
    passengerName: Joi.string().required(),
    outbound: Joi.boolean().default(true),
  }),
});

module.exports = { isBookingRequest };